package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpOutputFormat
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpVariant
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationConfig
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.otp.generation.toGenerationConfig
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicySnapshotReader
import com.aozijx.passly.runtime.session.SessionLockedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpCodeRuntimeTest {
    @Test
    fun `runtime owns one collection of each application signal`() =
        runTest {
            var unlockCollectors = 0
            var authenticationCollectors = 0
            val signals =
                OtpRuntimeSignals(
                    unlocked =
                        flow {
                            unlockCollectors++
                            emit(true)
                            awaitCancellation()
                        },
                    freshAuthentication =
                        flow {
                            authenticationCollectors++
                            awaitCancellation()
                        },
                )
            val runtime =
                testRuntime(scope = backgroundScope, loadOtpConfig = { validTotpConfig() }, codeGenerator = { OtpResult.Success("unused") })

            runtime.start(signals)
            runtime.start(signals)
            runCurrent()

            assertEquals(1, unlockCollectors)
            assertEquals(1, authenticationCollectors)
        }

    @Test
    fun `copy reads policy on demand and retries a failed read on the next action`() =
        runTest {
            var nowMillis = 26_000L
            var policyReads = 0
            var generatedCodes = 0
            val policyReader =
                object : OtpRuntimePolicySnapshotReader {
                    override suspend fun readOtpRuntimePolicySnapshot(): OtpRuntimePolicy {
                        policyReads++
                        if (policyReads == 1) throw IllegalStateException("temporary settings read failure")
                        return OtpRuntimePolicy(copyNextTotpNearExpiry = true)
                    }
                }
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generatedCodes++
                        OtpResult.Success("code-$generatedCodes")
                    },
                    otpRuntimePolicyReader = policyReader,
                )
            runtime.acquire("settings-retry")
            runCurrent()

            assertEquals(0, policyReads)
            assertNull(runtime.codeForCopy("settings-retry"))
            assertEquals(1, policyReads)

            nowMillis = 27_000L
            assertEquals("code-2", runtime.codeForCopy("settings-retry"))
            assertEquals(2, policyReads)
        }

    @Test
    fun `HOTP copy maps a transient consume exception to unavailable`() =
        runTest {
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = 1) },
                    codeGenerator = { generateTestOtp(it) },
                    consumeHotpCode = { _, _ -> throw IllegalStateException("temporary HOTP transaction failure") },
                )
            runtime.acquire("hotp-copy-error")
            runCurrent()

            assertNull(runtime.codeForCopy("hotp-copy-error"))
        }

    @Test
    fun `on-demand HOTP copy maps a transient consume exception to unavailable`() =
        runTest {
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = 1) },
                    codeGenerator = { generateTestOtp(it) },
                    consumeHotpCode = { _, _ -> throw IllegalStateException("temporary HOTP transaction failure") },
                )

            assertNull(runtime.codeForCopy("hotp-on-demand-error"))
        }

    @Test
    fun `on-demand TOTP copy does not return a code after session locks during generation`() =
        runTest {
            val generationStarted = CompletableDeferred<Unit>()
            val generationGate = CompletableDeferred<OtpResult>()
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { 27_000L },
                    loadOtpMaterial = { generationMaterialForTest(validTotpConfig()) },
                    codeGenerator = { _, _ ->
                        generationStarted.complete(Unit)
                        generationGate.await()
                    },
                )
            val copy = async { runtime.codeForCopy("unleased") }
            runCurrent()
            generationStarted.await()

            runtime.onSessionStateChanged(unlocked = false)
            generationGate.complete(OtpResult.Success("123456"))

            assertNull(copy.await())
        }

    @Test
    fun `on-demand HOTP copy does not return a code after session locks during consumption`() =
        runTest {
            val consumptionStarted = CompletableDeferred<Unit>()
            val consumptionGate = CompletableDeferred<OtpResult>()
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    codeGenerator = { _, _ -> OtpResult.Success("unused") },
                    loadOtpMaterial = {
                        generationMaterialForTest(OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = 1))
                    },
                    consumeHotpCode = { _, _ ->
                        consumptionStarted.complete(Unit)
                        consumptionGate.await()
                    },
                )
            val copy = async { runtime.codeForCopy("unleased-hotp") }
            runCurrent()
            consumptionStarted.await()

            runtime.onSessionStateChanged(unlocked = false)
            consumptionGate.complete(OtpResult.Success("123456"))

            assertNull(copy.await())
        }

    @Test
    fun `HOTP copy propagates cancellation and fatal errors`() =
        runTest {
            val failures =
                listOf(
                    CancellationException("HOTP copy cancelled"),
                    AssertionError("fatal HOTP consume failure"),
                )
            failures.forEachIndexed { index, failure ->
                val runtime =
                    testRuntime(
                        scope = backgroundScope,
                        loadOtpConfig = { OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = 1) },
                        codeGenerator = { generateTestOtp(it) },
                        consumeHotpCode = { _, _ -> throw failure },
                    )
                runtime.acquire("hotp-propagation-$index")
                runCurrent()

                val thrown =
                    try {
                        runtime.codeForCopy("hotp-propagation-$index")
                        null
                    } catch (caught: Throwable) {
                        caught
                    }
                assertTrue(thrown === failure)
            }
        }

    @Test
    fun `closing leases is idempotent and final owner stops automatic retry`() =
        runTest {
            var loadCount = 0
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { testScheduler.currentTime },
                    loadOtpConfig = {
                        loadCount++
                        throw IllegalStateException("temporary read failure")
                    },
                    codeGenerator = { OtpResult.Success("unused") },
                )
            runtime.start(OtpRuntimeSignals.NONE)
            val first = runtime.acquire("leased")
            val second = runtime.acquire("leased")
            runCurrent()
            assertEquals(1, loadCount)

            first.close()
            first.close()
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(2, loadCount)

            second.close()
            advanceTimeBy(30_000)
            runCurrent()
            assertEquals(2, loadCount)
            assertEquals(null, runtime.states.value["leased"])
        }

    @Test
    fun `crypto generation retry reuses seed and entry changes reload configuration`() =
        runTest {
            var nowMillis = 0L
            var loadCount = 0
            var generationCount = 0
            var config = validTotpConfig()
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = {
                        generationCount++
                        if (generationCount == 1) {
                            OtpResult.Failure(OtpGenerationError.CryptoError(IllegalStateException("temporary crypto error")))
                        } else {
                            generateTestOtp(it)
                        }
                    },
                )
            runtime.start(OtpRuntimeSignals.NONE)
            runtime.acquire("crypto-retry")
            runCurrent()
            assertEquals(OtpCodeState.Pending, runtime.states.value["crypto-retry"])

            nowMillis = 1_000
            advanceTimeBy(1_000)
            runCurrent()
            assertTrue(runtime.states.value["crypto-retry"] is OtpCodeState.TimedReady)
            assertEquals(1, loadCount)

            config = validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
            runtime.entryChanged("crypto-retry")
            runCurrent()
            assertTrue(runtime.states.value["crypto-retry"] is OtpCodeState.TimedReady)
            assertEquals(2, loadCount)
        }

    @Test
    fun `permanent invalid seed is not retried each tick and entry change restarts generation`() =
        runTest {
            var loadCount = 0
            var config = validTotpConfig().copy(secret = "!")
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { testScheduler.currentTime },
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = { generateTestOtp(it) },
                )
            runtime.start(OtpRuntimeSignals.NONE)
            runtime.acquire("permanent-failure")
            runCurrent()
            assertEquals(OtpCodeState.Failed(OtpGenerationError.InvalidSecret), runtime.states.value["permanent-failure"])

            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(1, loadCount)

            config = validTotpConfig()
            runtime.entryChanged("permanent-failure")
            runCurrent()
            assertTrue(runtime.states.value["permanent-failure"] is OtpCodeState.TimedReady)
            assertEquals(2, loadCount)
        }

    @Test
    fun `transient reads retry automatically with backoff and without a page refresh`() =
        runTest {
            var loadCount = 0
            val runtime =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { testScheduler.currentTime },
                    loadOtpConfig = {
                        loadCount++
                        if (loadCount < 3) throw IllegalStateException("temporary read failure")
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )
            runtime.start(OtpRuntimeSignals.NONE)
            runtime.acquire("auto-retry")
            runCurrent()
            assertEquals(1, loadCount)

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(2, loadCount)

            advanceTimeBy(1_999)
            runCurrent()
            assertEquals("backoff should prevent a one-second retry loop", 2, loadCount)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(3, loadCount)
            assertEquals("654321", runtime.readyCode("auto-retry"))
            assertEquals("654321", runtime.codeForCopy("auto-retry"))
        }

    @Test
    fun `exhausted HOTP counter becomes failed state without publishing a code`() =
        runTest {
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = Long.MAX_VALUE) },
                    codeGenerator = { generateTestOtp(it) },
                    consumeHotpCode = { _, expectedCounter ->
                        assertEquals(Long.MAX_VALUE, expectedCounter)
                        generateTestOtp(
                            OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = expectedCounter),
                        )
                    },
                )
            coordinator.acquire("exhausted")
            runCurrent()
            val result = coordinator.codeForCopy("exhausted")

            assertNull(result)
            assertEquals(OtpCodeState.Failed(OtpGenerationError.CounterExhausted), coordinator.states.value["exhausted"])
        }

    @Test
    fun `copy selection follows the latest policy snapshot in the final three seconds`() =
        runTest {
            var nowMillis = 26_000L
            var loads = 0
            var generated = 0
            val policy = MutableStateFlow(OtpRuntimePolicy())
            val policyReader =
                object : OtpRuntimePolicySnapshotReader {
                    override suspend fun readOtpRuntimePolicySnapshot() = policy.value
                }
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = {
                        loads++
                        validTotpConfig()
                    },
                    codeGenerator = {
                        generated++
                        OtpResult.Success("code-$generated")
                    },
                    otpRuntimePolicyReader = policyReader,
                )

            coordinator.acquire("near-expiry")
            runCurrent()
            val initial = coordinator.states.value.getValue("near-expiry") as OtpCodeState.TimedReady
            assertEquals("code-1", initial.code)
            assertEquals("code-2", initial.nextCode)
            assertEquals("code-1", coordinator.codeForCopy("near-expiry"))

            nowMillis = 27_000L
            coordinator.refreshStates(nowSeconds = 27)
            assertEquals("code-2", coordinator.codeForCopy("near-expiry"))

            policy.value = OtpRuntimePolicy(copyNextTotpNearExpiry = false)
            assertEquals("code-1", coordinator.codeForCopy("near-expiry"))
            assertEquals(1, loads)
        }

    @Test
    fun `copy uses current time when cached phase has not ticked yet`() =
        runTest {
            var nowMillis = 26_000L
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { nowMillis },
                    loadOtpMaterial = { testMaterial(validTotpConfig()) },
                    codeGenerator = { _, timestamp -> OtpResult.Success("at-$timestamp") },
                )
            runtime.acquire("delayed-tick")
            runCurrent()

            nowMillis = 28_000L

            assertEquals("at-30", runtime.codeForCopy("delayed-tick"))
        }

    @Test
    fun `copy uses the retained seed when cached schedule belongs to an earlier window`() =
        runTest {
            var nowMillis = 29_000L
            var loads = 0
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { nowMillis },
                    loadOtpMaterial = {
                        loads++
                        testMaterial(validTotpConfig())
                    },
                    codeGenerator = { _, timestamp -> OtpResult.Success("at-$timestamp") },
                )
            runtime.acquire("delayed-window-refresh")
            runCurrent()

            nowMillis = 57_000L

            assertEquals("at-60", runtime.codeForCopy("delayed-window-refresh"))
            assertEquals(1, loads)
        }

    @Test
    fun `on-demand copy recalculates if generation crosses a time window`() =
        runTest {
            var nowMillis = 29_999L
            var generated = 0
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { nowMillis },
                    loadOtpMaterial = { testMaterial(validTotpConfig()) },
                    codeGenerator = { _, timestamp ->
                        generated++
                        if (generated == 1) nowMillis = 30_000L
                        OtpResult.Success("at-$timestamp")
                    },
                )

            assertEquals("at-30", runtime.codeForCopy("window-race"))
            assertEquals(3, generated)
        }

    @Test
    fun `time window transition promotes next and reuses the lease seed`() =
        runTest {
            var nowMillis = 29_000L
            var loads = 0
            var generated = 0
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = {
                        loads++
                        validTotpConfig()
                    },
                    codeGenerator = {
                        generated++
                        OtpResult.Success("code-$generated")
                    },
                )
            coordinator.acquire("window")
            runCurrent()
            val before = coordinator.states.value.getValue("window") as OtpCodeState.TimedReady
            assertEquals("code-1", before.code)
            assertEquals("code-2", before.nextCode)

            nowMillis = 30_000L
            coordinator.refreshStates(nowSeconds = 30)
            val after = coordinator.states.value.getValue("window") as OtpCodeState.TimedReady
            assertEquals("code-3", after.code)
            assertEquals("code-4", after.nextCode)
            assertEquals(1, loads)
        }

    @Test
    fun `custom TOTP period applies the final three second copy phase`() =
        runTest {
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { 7_000L },
                    loadOtpMaterial = { generationMaterialForTest(validTotpConfig().copy(periodSeconds = 10)) },
                    codeGenerator = { _, timestamp -> OtpResult.Success("at-$timestamp") },
                )
            coordinator.acquire("custom-period")
            runCurrent()

            val state = coordinator.states.value.getValue("custom-period") as OtpCodeState.TimedReady
            assertEquals("at-7", state.code)
            assertEquals("at-10", state.nextCode)
            assertEquals("at-10", coordinator.codeForCopy("custom-period"))
        }

    @Test
    fun `Steam pre-generates current and next window codes`() =
        runTest {
            val seed = "JBSWY3DPEHPK3PXP".toByteArray()
            val material = OtpGenerationMaterial(SteamDefinition.configuration(secret = "unused").toGenerationConfig(), seed)
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { 27_000L },
                    loadOtpMaterial = { material },
                    codeGenerator = { value, timestamp -> OtpGenerator.generate(value, timestamp = timestamp) },
                )
            runtime.acquire("steam")
            runCurrent()

            val state = runtime.states.value.getValue("steam") as OtpCodeState.TimedReady
            assertEquals(5, state.code.length)
            assertEquals(5, state.nextCode.length)
            assertTrue(state.code != state.nextCode)
            assertTrue(seed.all { it == 0.toByte() })
        }

    @Test
    fun `generation failure still clears caller-owned seed material`() =
        runTest {
            val seed = "JBSWY3DPEHPK3PXP".toByteArray()
            val fatal = AssertionError("code generation failed")
            val reported = mutableListOf<Throwable>()
            val scope =
                CoroutineScope(
                    SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, failure -> reported += failure },
                )
            val runtime =
                OtpCodeRuntime(
                    scope = scope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    loadOtpMaterial = { OtpGenerationMaterial(validTotpConfig().toGenerationConfig(), seed) },
                    codeGenerator = { _, _ -> throw fatal },
                )

            runtime.acquire("failure")

            assertTrue(seed.all { it == 0.toByte() })
            assertEquals(listOf(fatal), reported)
            scope.cancel()
        }

    @Test
    fun `overlapping leases retain shared state until the final owner releases`() =
        runBlocking {
            var loads = 0
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loads++
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )
            val detailLease = coordinator.acquire("shared")
            val secondDetailLease = coordinator.acquire("shared")
            val listLease = coordinator.acquire("shared")
            assertEquals(1, loads)
            detailLease.close()
            listLease.close()
            assertEquals("654321", coordinator.readyCode("shared"))
            secondDetailLease.close()
            assertEquals(null, coordinator.states.value["shared"])
            scope.cancel()
        }

    @Test
    fun `copy for hidden OTP loads and clears seed without retaining runtime state`() =
        runTest {
            val nowSeconds = 27L
            val seed = "JBSWY3DPEHPK3PXP".toByteArray()
            val material = OtpGenerationMaterial(validTotpConfig().toGenerationConfig(), seed)
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    currentTimeMillis = { nowSeconds * 1_000 },
                    loadOtpMaterial = { material },
                    codeGenerator = { _, timestamp -> OtpResult.Success("window-$timestamp") },
                )

            assertEquals("window-30", runtime.codeForCopy("hidden"))
            assertTrue(seed.all { it == 0.toByte() })
            assertEquals(emptyMap<String, OtpCodeState>(), runtime.states.value)
        }

    @Test
    fun `copying HOTP consumes and returns a fresh code each time`() =
        runTest {
            var counter = 7L
            var preGenerated = 0
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig.Hotp(secret = "secret", counter = counter) },
                    codeGenerator = {
                        preGenerated++
                        OtpResult.Success("unused")
                    },
                    consumeHotpCode = { _, expectedCounter ->
                        assertEquals(counter, expectedCounter)
                        val usedCounter = expectedCounter
                        counter = expectedCounter + 1
                        OtpResult.CounterSuccess("code-$usedCounter", nextCounter = counter)
                    },
                )
            coordinator.acquire("hotp")
            runCurrent()
            assertEquals(1, preGenerated)

            assertEquals("code-7", coordinator.codeForCopy("hotp"))
            assertEquals("code-8", coordinator.codeForCopy("hotp"))
            assertEquals(
                OtpCodeState.HotpReady("unused", counter = 9, canRollbackPrevious = true),
                coordinator.states.value["hotp"],
            )
        }

    @Test
    fun `HOTP result is discarded when runtime closes during consumption`() =
        runTest {
            val consumptionStarted = CompletableDeferred<Unit>()
            val finishConsumption = CompletableDeferred<OtpResult>()
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig.Hotp(secret = "secret", counter = 7) },
                    codeGenerator = { OtpResult.Success("unused") },
                    consumeHotpCode = { _, _ ->
                        consumptionStarted.complete(Unit)
                        finishConsumption.await()
                    },
                )
            coordinator.acquire("hotp")
            runCurrent()
            val pendingCopy = async { coordinator.codeForCopy("hotp") }
            consumptionStarted.await()

            coordinator.onSessionStateChanged(unlocked = false)
            finishConsumption.complete(OtpResult.CounterSuccess("123456", nextCounter = 8))

            assertEquals(null, pendingCopy.await())
            assertEquals(emptyMap<String, OtpCodeState>(), coordinator.states.value)
        }

    @Test
    fun `stale HOTP authorization failures cannot clear a newer config`() {
        listOf<() -> Exception>(
            { FreshAuthenticationRequiredException() },
            { SessionLockedException("stale HOTP consume") },
        ).forEach(::assertStaleHotpFailureKeepsNewCode)
    }

    @Test
    fun `fresh authentication failure pauses ticker retries until lifecycle changes`() =
        runTest {
            var loadCount = 0
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = {
                        loadCount++
                        throw FreshAuthenticationRequiredException()
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.acquire("paused-entry")
            coordinator.start(OtpRuntimeSignals.NONE)
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()

            assertEquals(1, loadCount)
        }

    @Test
    fun `failed moving factor generation retries automatically from the lease seed`() =
        runTest {
            var loadCount = 0
            var generationCount = 0
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { testScheduler.currentTime },
                    loadOtpConfig = {
                        loadCount++
                        validTotpConfig().copy(periodSeconds = 1)
                    },
                    codeGenerator = {
                        generationCount++
                        if (generationCount == 3) {
                            OtpResult.Failure(OtpGenerationError.CryptoError(IllegalStateException("temporary crypto failure")))
                        } else {
                            OtpResult.Success("code-$generationCount")
                        }
                    },
                )
            coordinator.start(OtpRuntimeSignals.NONE)
            coordinator.acquire("reload-entry")
            runCurrent()
            assertEquals("code-1", coordinator.readyCode("reload-entry"))

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(1, loadCount)
            assertEquals(OtpCodeState.Pending, coordinator.states.value["reload-entry"])

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(1, loadCount)
            assertEquals("code-4", coordinator.readyCode("reload-entry"))
        }

    @Test
    fun `ticker continues after one entry fails and retries it on the next period`() =
        runTest {
            var nowMillis = 0L
            val loadCounts = mutableMapOf<String, Int>()
            val generationCounts = mutableMapOf<String, Int>()
            val coordinator =
                testRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = { entryId ->
                        val loadCount = (loadCounts[entryId] ?: 0) + 1
                        loadCounts[entryId] = loadCount
                        validTotpConfig().copy(
                            secret = "$entryId-$loadCount",
                            periodSeconds = 1,
                        )
                    },
                    codeGenerator = { config ->
                        val secret = requireNotNull(config.secret)
                        val generated = (generationCounts[secret] ?: 0) + 1
                        generationCounts[secret] = generated
                        if (secret.startsWith("failing-entry") && generated == 3) {
                            OtpResult.Failure(OtpGenerationError.CryptoError(IllegalStateException("temporary crypto failure")))
                        } else {
                            OtpResult.Success(secret)
                        }
                    },
                )
            coordinator.acquire("failing-entry")
            coordinator.acquire("healthy-entry")
            runCurrent()
            coordinator.start(OtpRuntimeSignals.NONE)

            nowMillis = 1_000
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(null, coordinator.readyCode("failing-entry"))
            assertEquals("healthy-entry-1", coordinator.readyCode("healthy-entry"))

            nowMillis = 2_000
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals("failing-entry-1", coordinator.readyCode("failing-entry"))
            assertEquals("healthy-entry-1", coordinator.readyCode("healthy-entry"))
        }

    @Test
    fun `lease activation propagates fatal errors and leaves cancellation unhandled`() =
        runTest {
            val fatal = AssertionError("fatal OTP config failure")
            val cancellation = CancellationException("activation cancelled")
            val reported = mutableListOf<Throwable>()
            val activationScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined +
                        CoroutineExceptionHandler { _, failure -> reported += failure },
                )
            val cancelledRuntime =
                testRuntime(
                    scope = activationScope,
                    loadOtpConfig = { throw cancellation },
                    codeGenerator = { OtpResult.Success("unused") },
                )
            val fatalRuntime =
                testRuntime(
                    scope = activationScope,
                    loadOtpConfig = { throw fatal },
                    codeGenerator = { OtpResult.Success("unused") },
                )

            cancelledRuntime.acquire("cancelled")
            fatalRuntime.acquire("fatal")

            assertEquals(listOf(fatal), reported)
            assertEquals(emptyMap<String, OtpCodeState>(), cancelledRuntime.states.value)
            activationScope.cancel()
        }

    @Test
    fun `fresh authentication signal resumes an unchanged active lease`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var freshAuthenticationAvailable = false
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        if (!freshAuthenticationAvailable) throw FreshAuthenticationRequiredException()
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )

            coordinator.acquire("still-visible-entry")
            assertEquals(null, coordinator.states.value["still-visible-entry"])

            freshAuthenticationAvailable = true
            coordinator.onFreshAuthentication()
            yield()

            assertEquals("654321", coordinator.readyCode("still-visible-entry"))
            scope.cancel()
        }

    @Test
    fun `changed visible entry reloads config without becoming permanently active`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var loadCount = 0
            var config = validTotpConfig()
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )

            val lease = coordinator.acquire("visible-entry")
            config = validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
            coordinator.entryChanged("visible-entry")

            assertEquals(2, loadCount)
            assertEquals("code-2", coordinator.readyCode("visible-entry"))
            lease.close()
            assertEquals(null, coordinator.states.value["visible-entry"])
            scope.cancel()
        }

    @Test
    fun `ticker reactivation while config read is pending does not duplicate the load`() =
        runTest {
            val scope = backgroundScope
            val configGate = CompletableDeferred<Unit>()
            var loadCount = 0
            val coordinator =
                testRuntime(
                    scope = scope,
                    currentTimeMillis = { 0L },
                    loadOtpConfig = {
                        loadCount++
                        configGate.await()
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.acquire("pending-entry")
            runCurrent()
            coordinator.start(OtpRuntimeSignals.NONE)
            advanceTimeBy(1_000)
            runCurrent()
            val loadsWhilePending = loadCount
            configGate.complete(Unit)
            runCurrent()

            assertEquals(1, loadsWhilePending)
            assertEquals("123456", coordinator.readyCode("pending-entry"))
        }

    @Test
    fun `entry change starts a new generation while old config read is pending`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldConfigGate = CompletableDeferred<Unit>()
            var loadCount = 0
            val oldConfig = validTotpConfig()
            val newConfig = oldConfig.copy(secret = "MFRGGZDFMZTWQ2LK")
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        val config = if (loadCount == 1) oldConfig else newConfig
                        if (loadCount == 1) oldConfigGate.await()
                        config
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )

            val lease = coordinator.acquire("changing-entry")
            coordinator.entryChanged("changing-entry")
            oldConfigGate.complete(Unit)
            yield()

            assertEquals(2, loadCount)
            assertEquals("code-2", coordinator.readyCode("changing-entry"))
            lease.close()
            scope.cancel()
        }

    @Test
    fun `stale config read authorization failures cannot clear a newer config`() {
        listOf(
            FreshAuthenticationRequiredException(),
            SessionLockedException("stale config read"),
        ).forEach(::assertStaleConfigReadFailureKeepsNewCode)
    }

    @Test
    fun `release before config load completes does not leave orphan refresh state`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val configGate = CompletableDeferred<Unit>()
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        configGate.await()
                        OtpConfig.Totp(
                            secret = "JBSWY3DPEHPK3PXP",
                            digits = 6,
                            periodSeconds = 30,
                            encoding = OtpSecretEncoding.BASE32,
                        )
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            val lease = coordinator.acquire("gone-entry")
            lease.close()
            configGate.complete(Unit)
            yield()

            assertEquals(null, coordinator.states.value["gone-entry"])
            scope.cancel()
        }

    @Test
    fun `release while code is generating does not restore disposed entry state`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val generationStarted = CompletableDeferred<Unit>()
            val generationGate = CompletableDeferred<Unit>()
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationStarted.complete(Unit)
                        generationGate.await()
                        OtpResult.Success("123456")
                    },
                )

            val lease = coordinator.acquire("gone-during-generation")
            generationStarted.await()
            lease.close()
            generationGate.complete(Unit)
            yield()

            assertEquals(null, coordinator.states.value["gone-during-generation"])
            scope.cancel()
        }

    @Test
    fun `session lock while code is generating does not restore sensitive state`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val generationStarted = CompletableDeferred<Unit>()
            val generationGate = CompletableDeferred<Unit>()
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationStarted.complete(Unit)
                        generationGate.await()
                        OtpResult.Success("123456")
                    },
                )

            coordinator.acquire("locked-during-generation")
            generationStarted.await()
            coordinator.onSessionStateChanged(unlocked = false)
            generationGate.complete(Unit)
            yield()

            assertEquals(emptyMap<String, OtpCodeState>(), coordinator.states.value)
            scope.cancel()
        }

    @Test
    fun `pre-lock generation cannot overwrite reactivated state after unlock`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldGenerationStarted = CompletableDeferred<Unit>()
            val oldGenerationGate = CompletableDeferred<Unit>()
            var generationCount = 0
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationCount++
                        if (generationCount == 1) {
                            oldGenerationStarted.complete(Unit)
                            oldGenerationGate.await()
                            OtpResult.Success("111111")
                        } else {
                            OtpResult.Success("222222")
                        }
                    },
                )

            coordinator.acquire("lock-cycle-entry")
            oldGenerationStarted.await()
            coordinator.onSessionStateChanged(unlocked = false)
            coordinator.onSessionStateChanged(unlocked = true)
            assertEquals("222222", coordinator.readyCode("lock-cycle-entry"))

            oldGenerationGate.complete(Unit)
            yield()

            assertEquals("222222", coordinator.readyCode("lock-cycle-entry"))
            scope.cancel()
        }

    @Test
    fun `disposed generation cannot overwrite a new lease lifecycle`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldGenerationStarted = CompletableDeferred<Unit>()
            val oldGenerationGate = CompletableDeferred<Unit>()
            var generationCount = 0
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationCount++
                        if (generationCount == 1) {
                            oldGenerationStarted.complete(Unit)
                            oldGenerationGate.await()
                            OtpResult.Success("111111")
                        } else {
                            OtpResult.Success("222222")
                        }
                    },
                )

            val oldLease = coordinator.acquire("replaced-lease-entry")
            oldGenerationStarted.await()
            oldLease.close()
            val replacementLease = coordinator.acquire("replaced-lease-entry")
            assertEquals("222222", coordinator.readyCode("replaced-lease-entry"))

            oldGenerationGate.complete(Unit)
            yield()

            assertEquals("222222", coordinator.readyCode("replaced-lease-entry"))
            replacementLease.close()
            scope.cancel()
        }

    @Test
    fun `lease activation generates Steam code from persisted material by entry id`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val storedConfig =
                SteamDefinition.configuration(
                    secret = "JBSWY3DPEHPK3PXP",
                    encoding = OtpSecretEncoding.BASE32,
                )
            var loadedEntryId: String? = null
            var generatedConfig: OtpGenerationConfig? = null
            val runtime =
                OtpCodeRuntime(
                    scope = scope,
                    currentTimeMillis = { 1_000_000_000L },
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    loadOtpMaterial = { entryId ->
                        loadedEntryId = entryId
                        generationMaterialForTest(storedConfig)
                    },
                    codeGenerator = { material, timestamp ->
                        generatedConfig = material.config
                        OtpGenerator.generate(material, timestamp = timestamp)
                    },
                )

            try {
                runtime.acquire("steam-entry")

                assertEquals("steam-entry", loadedEntryId)
                assertTrue(generatedConfig is OtpGenerationConfig.Totp)
                assertEquals(OtpHashAlgorithm.SHA1, generatedConfig?.algorithm)
                assertEquals(5, generatedConfig?.digits)
                assertEquals(OtpOutputFormat.STEAM_ALPHABET, generatedConfig?.outputFormat)
                assertEquals(TotpVariant.STEAM, (generatedConfig as OtpGenerationConfig.Totp).variant)
                assertEquals(OtpSecretEncoding.BASE32, generatedConfig?.encoding)
                assertEquals("38CTQ", runtime.readyCode("steam-entry"))
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun `session lock during activation clears state without throwing`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        throw SessionLockedException("Session is SOFT_LOCKED")
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.acquire("locked-entry")

            assertEquals(emptyMap<String, OtpCodeState>(), coordinator.states.value)
            scope.cancel()
        }

    @Test
    fun `unlock reactivates entry tracked while session was locked`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var loadCount = 0
            val config =
                OtpConfig.Totp(
                    secret = "JBSWY3DPEHPK3PXP",
                    digits = 6,
                    periodSeconds = 30,
                    encoding = OtpSecretEncoding.BASE32,
                )
            val coordinator =
                testRuntime(
                    scope = scope,
                    initiallyUnlocked = false,
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.acquire("pending-entry")
            assertEquals(0, loadCount)

            coordinator.onSessionStateChanged(unlocked = true)

            assertEquals(1, loadCount)
            assertEquals("123456", coordinator.readyCode("pending-entry"))
            scope.cancel()
        }

    private fun OtpCodeRuntime.readyCode(entryId: String): String? =
        when (val state = states.value[entryId]) {
            is OtpCodeState.TimedReady -> state.code
            is OtpCodeState.HotpReady -> state.code
            OtpCodeState.Pending, is OtpCodeState.Failed, null -> null
        }

    private fun testRuntime(
        scope: CoroutineScope,
        loadOtpConfig: suspend (String) -> OtpConfig?,
        codeGenerator: suspend (OtpConfig) -> OtpResult,
        consumeHotpCode: suspend (String, Long) -> OtpResult = { _, _ -> OtpResult.Failure(OtpGenerationError.InvalidSecret) },
        initiallyUnlocked: Boolean = true,
        currentTimeMillis: () -> Long = System::currentTimeMillis,
        otpRuntimePolicyReader: OtpRuntimePolicySnapshotReader = defaultOtpRuntimePolicyReader(),
    ) = OtpCodeRuntime(
        scope = scope,
        otpRuntimePolicyReader = otpRuntimePolicyReader,
        loadOtpMaterial = { entryId -> loadOtpConfig(entryId)?.let(::testMaterial) },
        codeGenerator = { material, _ ->
            val secret = String(material.secretBytes, Charsets.UTF_8)
            val config = material.config
            codeGenerator(
                when (config) {
                    is OtpGenerationConfig.Totp -> {
                        OtpConfig.Totp(
                            secret,
                            config.algorithm,
                            config.digits,
                            config.periodSeconds,
                            config.encoding,
                            variant = config.variant,
                        )
                    }

                    is OtpGenerationConfig.Hotp -> {
                        OtpConfig.Hotp(secret, config.algorithm, config.digits, config.counter, config.encoding)
                    }
                },
            )
        },
        consumeHotpCode = consumeHotpCode,
        initiallyUnlocked = initiallyUnlocked,
        currentTimeMillis = currentTimeMillis,
    )

    private fun testMaterial(config: OtpConfig) =
        OtpGenerationMaterial(
            config = config.toGenerationConfig(),
            secretBytes = config.secret?.toByteArray(Charsets.UTF_8) ?: byteArrayOf(),
        )

    private fun generateTestOtp(config: OtpConfig): OtpResult = testMaterial(config).use { material -> OtpGenerator.generate(material) }

    private fun assertStaleConfigReadFailureKeepsNewCode(failure: Exception) =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldConfigGate = CompletableDeferred<Unit>()
            var loadCount = 0
            val coordinator =
                testRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        if (loadCount == 1) {
                            oldConfigGate.await()
                            throw failure
                        }
                        validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )

            try {
                coordinator.acquire("stale-config-entry")
                coordinator.entryChanged("stale-config-entry")
                assertEquals("code-2", coordinator.readyCode("stale-config-entry"))
                oldConfigGate.complete(Unit)
                yield()

                assertEquals("code-2", coordinator.readyCode("stale-config-entry"))
            } finally {
                scope.cancel()
            }
        }

    private fun assertStaleHotpFailureKeepsNewCode(failure: () -> Exception) =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val consumeGate = CompletableDeferred<Unit>()
                val consumeStarted = CompletableDeferred<Unit>()
                var loadCount = 0
                val coordinator =
                    testRuntime(
                        scope = scope,
                        loadOtpConfig = {
                            loadCount++
                            if (loadCount == 1) {
                                OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = 7)
                            } else {
                                validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
                            }
                        },
                        codeGenerator = { OtpResult.Success("code-$loadCount") },
                        consumeHotpCode = { _, _ ->
                            consumeStarted.complete(Unit)
                            consumeGate.await()
                            throw failure()
                        },
                    )
                coordinator.acquire("changing-HOTP-entry")
                val staleCopy = scope.async { coordinator.codeForCopy("changing-HOTP-entry") }
                consumeStarted.await()

                coordinator.entryChanged("changing-HOTP-entry")
                assertEquals("code-2", coordinator.readyCode("changing-HOTP-entry"))

                consumeGate.complete(Unit)
                assertEquals(null, staleCopy.await())
                assertEquals("code-2", coordinator.readyCode("changing-HOTP-entry"))
            } finally {
                scope.cancel()
            }
        }

    private fun validTotpConfig() =
        OtpConfig.Totp(
            secret = "JBSWY3DPEHPK3PXP",
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32,
        )
}

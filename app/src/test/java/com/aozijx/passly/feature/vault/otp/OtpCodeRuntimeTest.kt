package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.runtime.session.SessionLockedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpCodeRuntimeTest {
    @Test
    fun `exhausted HOTP counter becomes failed state without publishing a code`() =
        runTest {
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig(type = OtpType.HOTP, secret = "JBSWY3DPEHPK3PXP", counter = Long.MAX_VALUE) },
                    codeGenerator = { OtpGenerator.generate(it) },
                    consumeHotpCode = {
                        OtpGenerator.generate(
                            OtpConfig(type = OtpType.HOTP, secret = "JBSWY3DPEHPK3PXP", counter = Long.MAX_VALUE),
                        )
                    },
                )
            coordinator.subscribe("exhausted")
            runCurrent()
            val result = coordinator.generateHotpCode("exhausted")

            assertEquals(OtpResult.Failure(OtpGenerationError.CounterExhausted), result)
            assertEquals(OtpCodeState.Failed(OtpGenerationError.CounterExhausted), coordinator.states.value["exhausted"])
        }

    @Test
    fun `copying HOTP consumes and returns a fresh code each time`() =
        runTest {
            var counter = 7L
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig(type = OtpType.HOTP, secret = "secret", counter = counter) },
                    codeGenerator = { OtpResult.Success("unused") },
                    consumeHotpCode = {
                        val usedCounter = counter++
                        OtpResult.Success("code-$usedCounter", nextCounter = counter)
                    },
                )
            coordinator.subscribe("hotp")
            runCurrent()

            assertEquals("code-7", coordinator.codeForCopy("hotp"))
            assertEquals("code-8", coordinator.codeForCopy("hotp"))
            assertEquals(OtpCodeState.Ready("code-8", 0f), coordinator.states.value["hotp"])
        }

    @Test
    fun `HOTP result is discarded when runtime closes during consumption`() =
        runTest {
            val consumptionStarted = CompletableDeferred<Unit>()
            val finishConsumption = CompletableDeferred<OtpResult>()
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = { OtpConfig(type = OtpType.HOTP, secret = "secret", counter = 7) },
                    codeGenerator = { OtpResult.Success("unused") },
                    consumeHotpCode = {
                        consumptionStarted.complete(Unit)
                        finishConsumption.await()
                    },
                )
            coordinator.subscribe("hotp")
            runCurrent()
            val pendingCopy = async { coordinator.codeForCopy("hotp") }
            consumptionStarted.await()

            coordinator.clearAllSensitiveState()
            finishConsumption.complete(OtpResult.Success("123456", nextCounter = 8))

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
                OtpCodeRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = {
                        loadCount++
                        throw FreshAuthenticationRequiredException()
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.subscribe("paused-entry")
            coordinator.start()
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()

            assertEquals(1, loadCount)
        }

    @Test
    fun `failed moving factor reload clears stale code and retries on the next refresh`() =
        runTest {
            val nowSeconds = 30L
            var loadCount = 0
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowSeconds * 1_000 },
                    loadOtpConfig = {
                        loadCount++
                        if (loadCount == 2) throw IllegalStateException("temporary config read failure")
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )
            coordinator.subscribe("reload-entry")
            runCurrent()
            val initialMovingFactor = nowSeconds / 30

            coordinator.refreshStates(nowSeconds = (initialMovingFactor + 1) * 30)

            assertEquals(null, coordinator.codeForCopy("reload-entry"))
            assertEquals(null, coordinator.states.value["reload-entry"])

            coordinator.refreshStates(nowSeconds = (initialMovingFactor + 2) * 30)

            assertEquals("code-3", coordinator.codeForCopy("reload-entry"))
            assertEquals(3, loadCount)
        }

    @Test
    fun `ticker continues after one entry fails and retries it on the next period`() =
        runTest {
            var nowMillis = 0L
            val loadCounts = mutableMapOf<String, Int>()
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    currentTimeMillis = { nowMillis },
                    loadOtpConfig = { entryId ->
                        val loadCount = (loadCounts[entryId] ?: 0) + 1
                        loadCounts[entryId] = loadCount
                        if (entryId == "failing-entry" && loadCount == 2) {
                            throw IllegalStateException("temporary config read failure")
                        }
                        validTotpConfig().copy(
                            secret = "$entryId-$loadCount",
                            periodSeconds = 1,
                        )
                    },
                    codeGenerator = { OtpResult.Success(requireNotNull(it.secret)) },
                )
            coordinator.subscribe("failing-entry")
            coordinator.subscribe("healthy-entry")
            runCurrent()
            coordinator.start()

            nowMillis = 1_000
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(null, coordinator.readyCode("failing-entry"))
            assertEquals("healthy-entry-2", coordinator.readyCode("healthy-entry"))

            nowMillis = 2_000
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals("failing-entry-3", coordinator.readyCode("failing-entry"))
            assertEquals("healthy-entry-3", coordinator.readyCode("healthy-entry"))
        }

    @Test
    fun `ordinary subscription activation read failure can be retried`() =
        runTest {
            var loadCount = 0
            val coordinator =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    loadOtpConfig = {
                        loadCount++
                        if (loadCount == 1) throw IllegalStateException("temporary config read failure")
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )

            coordinator.subscribe("activation-retry")
            runCurrent()
            coordinator.refreshRequestedEntries()
            runCurrent()

            assertEquals(2, loadCount)
            assertEquals("654321", coordinator.codeForCopy("activation-retry"))
        }

    @Test
    fun `subscription activation propagates fatal errors and leaves cancellation unhandled`() =
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
                OtpCodeRuntime(
                    scope = activationScope,
                    loadOtpConfig = { throw cancellation },
                    codeGenerator = { OtpResult.Success("unused") },
                )
            val fatalRuntime =
                OtpCodeRuntime(
                    scope = activationScope,
                    loadOtpConfig = { throw fatal },
                    codeGenerator = { OtpResult.Success("unused") },
                )

            cancelledRuntime.subscribe("cancelled")
            fatalRuntime.subscribe("fatal")

            assertEquals(listOf(fatal), reported)
            assertEquals(emptyMap<String, OtpCodeState>(), cancelledRuntime.states.value)
            activationScope.cancel()
        }

    @Test
    fun `fresh authentication signal resumes an unchanged visible subscription`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var freshAuthenticationAvailable = false
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        if (!freshAuthenticationAvailable) throw FreshAuthenticationRequiredException()
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )

            coordinator.subscribe("still-visible-entry")
            assertEquals(null, coordinator.states.value["still-visible-entry"])

            freshAuthenticationAvailable = true
            coordinator.onFreshAuthentication()
            yield()

            assertEquals("654321", coordinator.readyCode("still-visible-entry"))
            scope.cancel()
        }

    @Test
    fun `visible subscriptions activate once and clear after final release`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var loadCount = 0
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        OtpConfig(
                            type = OtpType.TOTP,
                            secret = "JBSWY3DPEHPK3PXP",
                            digits = 6,
                            periodSeconds = 30,
                            encoding = OtpSecretEncoding.BASE32,
                        )
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.subscribe("visible-entry")
            coordinator.subscribe("visible-entry")

            assertEquals(1, loadCount)
            assertEquals("123456", coordinator.readyCode("visible-entry"))

            coordinator.unsubscribe("visible-entry")
            assertEquals("123456", coordinator.readyCode("visible-entry"))

            coordinator.unsubscribe("visible-entry")
            assertEquals(null, coordinator.states.value["visible-entry"])
            scope.cancel()
        }

    @Test
    fun `releasing list subscription keeps explicitly activated detail entry`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        OtpConfig(
                            type = OtpType.TOTP,
                            secret = "JBSWY3DPEHPK3PXP",
                            digits = 6,
                            periodSeconds = 30,
                            encoding = OtpSecretEncoding.BASE32,
                        )
                    },
                    codeGenerator = { OtpResult.Success("654321") },
                )

            coordinator.autoUnlock("shared-entry")
            coordinator.subscribe("shared-entry")
            coordinator.unsubscribe("shared-entry")

            assertEquals("654321", coordinator.readyCode("shared-entry"))
            scope.cancel()
        }

    @Test
    fun `changed visible entry reloads config without becoming permanently active`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var loadCount = 0
            var config = validTotpConfig()
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )

            coordinator.subscribe("visible-entry")
            config = validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
            coordinator.entryChanged("visible-entry")

            assertEquals(2, loadCount)
            assertEquals("code-2", coordinator.readyCode("visible-entry"))
            coordinator.unsubscribe("visible-entry")
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
                OtpCodeRuntime(
                    scope = scope,
                    currentTimeMillis = { 0L },
                    loadOtpConfig = {
                        loadCount++
                        configGate.await()
                        validTotpConfig()
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.subscribe("pending-entry")
            runCurrent()
            coordinator.start()
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
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        loadCount++
                        val config = if (loadCount == 1) oldConfig else newConfig
                        if (loadCount == 1) oldConfigGate.await()
                        config
                    },
                    codeGenerator = { OtpResult.Success("code-$loadCount") },
                )

            coordinator.subscribe("changing-entry")
            coordinator.entryChanged("changing-entry")
            oldConfigGate.complete(Unit)
            yield()

            assertEquals(2, loadCount)
            assertEquals("code-2", coordinator.readyCode("changing-entry"))
            coordinator.unsubscribe("changing-entry")
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
    fun `stale refresh authorization failures cannot clear a newer config`() {
        listOf<() -> Exception>(
            { FreshAuthenticationRequiredException() },
            { SessionLockedException("stale refresh") },
        ).forEach(::assertStaleRefreshFailureKeepsNewCode)
    }

    @Test
    fun `release before config load completes does not leave orphan refresh state`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val configGate = CompletableDeferred<Unit>()
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        configGate.await()
                        OtpConfig(
                            type = OtpType.TOTP,
                            secret = "JBSWY3DPEHPK3PXP",
                            digits = 6,
                            periodSeconds = 30,
                            encoding = OtpSecretEncoding.BASE32,
                        )
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.subscribe("gone-entry")
            coordinator.unsubscribe("gone-entry")
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
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationStarted.complete(Unit)
                        generationGate.await()
                        OtpResult.Success("123456")
                    },
                )

            coordinator.subscribe("gone-during-generation")
            generationStarted.await()
            coordinator.unsubscribe("gone-during-generation")
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
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = { validTotpConfig() },
                    codeGenerator = {
                        generationStarted.complete(Unit)
                        generationGate.await()
                        OtpResult.Success("123456")
                    },
                )

            coordinator.subscribe("locked-during-generation")
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
                OtpCodeRuntime(
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

            coordinator.subscribe("lock-cycle-entry")
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
    fun `disposed generation cannot overwrite a new subscription lifecycle`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldGenerationStarted = CompletableDeferred<Unit>()
            val oldGenerationGate = CompletableDeferred<Unit>()
            var generationCount = 0
            val coordinator =
                OtpCodeRuntime(
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

            coordinator.subscribe("resubscribed-entry")
            oldGenerationStarted.await()
            coordinator.unsubscribe("resubscribed-entry")
            coordinator.subscribe("resubscribed-entry")
            assertEquals("222222", coordinator.readyCode("resubscribed-entry"))

            oldGenerationGate.complete(Unit)
            yield()

            assertEquals("222222", coordinator.readyCode("resubscribed-entry"))
            scope.cancel()
        }

    @Test
    fun `subscription loads complete Steam config by entry id`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val storedConfig =
                OtpConfig(
                    type = OtpType.STEAM,
                    secret = "JBSWY3DPEHPK3PXP",
                    digits = 5,
                    periodSeconds = 30,
                    encoding = OtpSecretEncoding.BASE32,
                    issuer = "Steam",
                )
            var loadedEntryId: String? = null
            var generatedConfig: OtpConfig? = null
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = { entryId ->
                        loadedEntryId = entryId
                        storedConfig
                    },
                    codeGenerator = { config ->
                        generatedConfig = config
                        OtpResult.Success("38CTQ")
                    },
                )

            coordinator.subscribe("steam-entry")

            assertEquals("steam-entry", loadedEntryId)
            assertNotNull(generatedConfig)
            assertEquals(OtpType.STEAM, generatedConfig?.type)
            assertEquals(OtpSecretEncoding.BASE32, generatedConfig?.encoding)
            assertEquals("JBSWY3DPEHPK3PXP", generatedConfig?.secret)
            assertEquals("38CTQ", coordinator.readyCode("steam-entry"))
            scope.cancel()
        }

    @Test
    fun `session lock during activation clears state without throwing`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    loadOtpConfig = {
                        throw SessionLockedException("Session is SOFT_LOCKED")
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.subscribe("locked-entry")

            assertEquals(emptyMap<String, OtpCodeState>(), coordinator.states.value)
            scope.cancel()
        }

    @Test
    fun `unlock reactivates entry tracked while session was locked`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            var loadCount = 0
            val config =
                OtpConfig(
                    type = OtpType.TOTP,
                    secret = "JBSWY3DPEHPK3PXP",
                    digits = 6,
                    periodSeconds = 30,
                    encoding = OtpSecretEncoding.BASE32,
                )
            val coordinator =
                OtpCodeRuntime(
                    scope = scope,
                    initiallyUnlocked = false,
                    loadOtpConfig = {
                        loadCount++
                        config
                    },
                    codeGenerator = { OtpResult.Success("123456") },
                )

            coordinator.autoUnlock("pending-entry")
            assertEquals(0, loadCount)

            coordinator.onSessionStateChanged(unlocked = true)

            assertEquals(1, loadCount)
            assertEquals("123456", coordinator.readyCode("pending-entry"))
            scope.cancel()
        }

    private fun OtpCodeRuntime.readyCode(entryId: String): String? = (states.value[entryId] as? OtpCodeState.Ready)?.code

    private fun assertStaleConfigReadFailureKeepsNewCode(failure: Exception) =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val oldConfigGate = CompletableDeferred<Unit>()
            var loadCount = 0
            val coordinator =
                OtpCodeRuntime(
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
                coordinator.subscribe("stale-config-entry")
                coordinator.entryChanged("stale-config-entry")
                assertEquals("code-2", coordinator.readyCode("stale-config-entry"))
                oldConfigGate.complete(Unit)
                yield()

                assertEquals("code-2", coordinator.readyCode("stale-config-entry"))
            } finally {
                scope.cancel()
            }
        }

    private fun assertStaleRefreshFailureKeepsNewCode(failure: () -> Exception) =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val nowSeconds = 30L
                val staleReadGate = CompletableDeferred<Unit>()
                var loadCount = 0
                val coordinator =
                    OtpCodeRuntime(
                        scope = scope,
                        currentTimeMillis = { nowSeconds * 1_000 },
                        loadOtpConfig = {
                            loadCount++
                            if (loadCount == 2) {
                                staleReadGate.await()
                                throw failure()
                            }
                            validTotpConfig().copy(secret = if (loadCount == 1) "JBSWY3DPEHPK3PXP" else "MFRGGZDFMZTWQ2LK")
                        },
                        codeGenerator = { OtpResult.Success("code-$loadCount") },
                    )
                coordinator.subscribe("refresh-entry")
                val nextMovingFactor = (nowSeconds / 30 + 1) * 30
                val oldRefresh = scope.launch { coordinator.refreshStates(nextMovingFactor) }
                coordinator.entryChanged("refresh-entry")

                assertEquals("code-3", coordinator.readyCode("refresh-entry"))
                staleReadGate.complete(Unit)
                oldRefresh.join()

                assertEquals("code-3", coordinator.readyCode("refresh-entry"))
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
                    OtpCodeRuntime(
                        scope = scope,
                        loadOtpConfig = {
                            loadCount++
                            if (loadCount == 1) {
                                OtpConfig(type = OtpType.HOTP, secret = "JBSWY3DPEHPK3PXP", counter = 7)
                            } else {
                                validTotpConfig().copy(secret = "MFRGGZDFMZTWQ2LK")
                            }
                        },
                        codeGenerator = { OtpResult.Success("code-$loadCount") },
                        consumeHotpCode = {
                            consumeStarted.complete(Unit)
                            consumeGate.await()
                            throw failure()
                        },
                    )
                coordinator.subscribe("changing-HOTP-entry")
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
        OtpConfig(
            type = OtpType.TOTP,
            secret = "JBSWY3DPEHPK3PXP",
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32,
        )
}

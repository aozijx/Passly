package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpCodeRefreshUseCaseTest {

    @Test
    fun `visible otp waits for fresh authentication without crashing`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = OtpCodeRefreshUseCase(
            scope = scope,
            loadOtpConfig = { throw FreshAuthenticationRequiredException() },
            codeGenerator = { OtpResult.Success("123456") },
        )

        coordinator.subscribe("fresh-auth-entry")
        yield()

        assertEquals(null, coordinator.states.value["fresh-auth-entry"])
        scope.cancel()
    }

    @Test
    fun `fresh authentication failure pauses ticker retries until lifecycle changes`() = runTest {
        var loadCount = 0
        val coordinator = OtpCodeRefreshUseCase(
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
    fun `fresh authentication signal resumes an unchanged visible subscription`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var freshAuthenticationAvailable = false
        val coordinator = OtpCodeRefreshUseCase(
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

        assertEquals("654321", coordinator.states.value["still-visible-entry"]?.code)
        scope.cancel()
    }

    @Test
    fun `visible subscriptions activate once and clear after final release`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var loadCount = 0
        val coordinator = OtpCodeRefreshUseCase(
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
        assertEquals("123456", coordinator.states.value["visible-entry"]?.code)

        coordinator.unsubscribe("visible-entry")
        assertEquals("123456", coordinator.states.value["visible-entry"]?.code)

        coordinator.unsubscribe("visible-entry")
        assertEquals(null, coordinator.states.value["visible-entry"])
        scope.cancel()
    }

    @Test
    fun `releasing list subscription keeps explicitly activated detail entry`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = OtpCodeRefreshUseCase(
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

        assertEquals("654321", coordinator.states.value["shared-entry"]?.code)
        scope.cancel()
    }

    @Test
    fun `release before config load completes does not leave orphan refresh state`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val configGate = CompletableDeferred<Unit>()
        val coordinator = OtpCodeRefreshUseCase(
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
    fun `release while code is generating does not restore disposed entry state`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val generationStarted = CompletableDeferred<Unit>()
        val generationGate = CompletableDeferred<Unit>()
        val coordinator = OtpCodeRefreshUseCase(
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
    fun `session lock while code is generating does not restore sensitive state`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val generationStarted = CompletableDeferred<Unit>()
        val generationGate = CompletableDeferred<Unit>()
        val coordinator = OtpCodeRefreshUseCase(
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

        assertEquals(emptyMap<String, Any>(), coordinator.states.value)
        scope.cancel()
    }

    @Test
    fun `pre-lock generation cannot overwrite reactivated state after unlock`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val oldGenerationStarted = CompletableDeferred<Unit>()
        val oldGenerationGate = CompletableDeferred<Unit>()
        var generationCount = 0
        val coordinator = OtpCodeRefreshUseCase(
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
        assertEquals("222222", coordinator.states.value["lock-cycle-entry"]?.code)

        oldGenerationGate.complete(Unit)
        yield()

        assertEquals("222222", coordinator.states.value["lock-cycle-entry"]?.code)
        scope.cancel()
    }

    @Test
    fun `disposed generation cannot overwrite a new subscription lifecycle`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val oldGenerationStarted = CompletableDeferred<Unit>()
        val oldGenerationGate = CompletableDeferred<Unit>()
        var generationCount = 0
        val coordinator = OtpCodeRefreshUseCase(
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
        assertEquals("222222", coordinator.states.value["resubscribed-entry"]?.code)

        oldGenerationGate.complete(Unit)
        yield()

        assertEquals("222222", coordinator.states.value["resubscribed-entry"]?.code)
        scope.cancel()
    }

    @Test
    fun `activate loads complete Steam config by entry id`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val storedConfig = OtpConfig(
            type = OtpType.STEAM,
            secret = "JBSWY3DPEHPK3PXP",
            digits = 5,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32,
            issuer = "Steam"
        )
        var loadedEntryId: String? = null
        var generatedConfig: OtpConfig? = null
        val coordinator = OtpCodeRefreshUseCase(
            scope = scope,
            loadOtpConfig = { entryId ->
                loadedEntryId = entryId
                storedConfig
            },
            codeGenerator = { config ->
                generatedConfig = config
                OtpResult.Success("38CTQ")
            }
        )

        coordinator.activate("steam-entry")

        assertEquals("steam-entry", loadedEntryId)
        assertNotNull(generatedConfig)
        assertEquals(OtpType.STEAM, generatedConfig?.type)
        assertEquals(OtpSecretEncoding.BASE32, generatedConfig?.encoding)
        assertEquals("JBSWY3DPEHPK3PXP", generatedConfig?.secret)
        assertEquals("38CTQ", coordinator.states.value["steam-entry"]?.code)
        scope.cancel()
    }

    @Test
    fun `session lock during activation clears state without throwing`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = OtpCodeRefreshUseCase(
            scope = scope,
            loadOtpConfig = {
                throw SessionLockedException("Session is SOFT_LOCKED")
            },
            codeGenerator = { OtpResult.Success("123456") }
        )

        coordinator.activate("locked-entry")

        assertEquals(emptyMap<String, Any>(), coordinator.states.value)
        scope.cancel()
    }

    @Test
    fun `unlock reactivates entry tracked while session was locked`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var loadCount = 0
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = "JBSWY3DPEHPK3PXP",
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )
        val coordinator = OtpCodeRefreshUseCase(
            scope = scope,
            initiallyUnlocked = false,
            loadOtpConfig = {
                loadCount++
                config
            },
            codeGenerator = { OtpResult.Success("123456") }
        )

        coordinator.autoUnlock("pending-entry")
        assertEquals(0, loadCount)

        coordinator.onSessionStateChanged(unlocked = true)

        assertEquals(1, loadCount)
        assertEquals("123456", coordinator.states.value["pending-entry"]?.code)
        scope.cancel()
    }

    private fun validTotpConfig() = OtpConfig(
        type = OtpType.TOTP,
        secret = "JBSWY3DPEHPK3PXP",
        digits = 6,
        periodSeconds = 30,
        encoding = OtpSecretEncoding.BASE32,
    )
}

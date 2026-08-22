package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TotpCoordinatorTest {

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
        val coordinator = TotpCoordinator(
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
        val coordinator = TotpCoordinator(
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
        val coordinator = TotpCoordinator(
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
}

package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType
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
}

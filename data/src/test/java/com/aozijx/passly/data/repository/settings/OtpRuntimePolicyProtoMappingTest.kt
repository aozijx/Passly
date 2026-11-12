package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpRuntimePolicyProtoMappingTest {
    @Test
    fun `next TOTP copy preference defaults on and round trips`() {
        val defaultPolicy = VaultViewPreferences.getDefaultInstance().toOtpRuntimePolicy()
        val disabledPolicy =
            VaultViewPreferences
                .newBuilder()
                .setCopyNextTotpNearExpiry(false)
                .build()
                .toOtpRuntimePolicy()

        assertTrue(defaultPolicy.copyNextTotpNearExpiry)
        assertEquals(OtpRuntimePolicy(copyNextTotpNearExpiry = false), disabledPolicy)
    }
}

package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.model.secret.WifiSecret
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryCapabilityFlagsTest {

    @Test
    fun loginPassword_setsPasswordCapability() {
        val flags = EntryCapabilityFlags.computeFrom(
            EntrySecret(
                login = LoginSecret(password = "password")
            )
        )

        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_PASSWORD))
    }

    @Test
    fun attachments_areExplicitAndIndependentFromSecret() {
        val withoutAttachment = EntryCapabilityFlags.computeFrom(EntrySecret())
        val withAttachment = EntryCapabilityFlags.computeFrom(
            secret = EntrySecret(),
            hasAttachments = true
        )

        assertFalse(
            EntryCapabilityFlags.has(
                withoutAttachment,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
        assertTrue(
            EntryCapabilityFlags.has(
                withAttachment,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
    }

    @Test
    fun wifiPassword_setsPasswordCapability() {
        val flags = EntryCapabilityFlags.computeFrom(
            EntrySecret(wifi = WifiSecret(password = "wifi-password"))
        )

        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_PASSWORD))
        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_WIFI))
    }
}

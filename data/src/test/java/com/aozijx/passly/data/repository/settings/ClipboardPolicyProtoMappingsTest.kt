package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardPolicyProtoMappingsTest {

    @Test
    fun `proto defaults map to enabled thirty second policy`() {
        val settings = readSecurity(SecurityPreferences.getDefaultInstance())

        assertTrue(settings.clipboardClearPolicy.enabled)
        assertEquals(30, settings.clipboardClearPolicy.delaySeconds)
    }

    @Test
    fun `unsupported stored delay maps to default delay`() {
        val proto = SecurityPreferences.newBuilder()
            .setClipboardClearEnabled(true)
            .setClipboardClearDelaySeconds(17)
            .build()

        val settings = readSecurity(proto)

        assertEquals(30, settings.clipboardClearPolicy.delaySeconds)
    }
}

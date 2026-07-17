package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.data.local.datastore.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ProtoSerializerTest {

    @Test
    fun appSettings_schemaOwnsNonZeroDefaults() {
        val defaults = AppSettingsSerializer.defaultValue

        assertEquals(60_000L, defaults.lockTimeoutMs)
        assertEquals(true, defaults.dynamicColor)
        assertEquals(true, defaults.secureContent)
        assertEquals(true, defaults.swipeEnabled)
        assertEquals("COPY_PASSWORD", defaults.swipeLeftAction)
        assertEquals("DETAIL", defaults.swipeRightAction)
        assertEquals(4, defaults.tabBarMaxTabsWithoutScroll)
        assertEquals(true, defaults.showGeneralMessages)
        assertFalse(defaults.hasShowGeneralMessages())
    }

    @Test
    fun appSettings_roundTripPreservesPresenceAndCollections() = runBlocking {
        val settings = AppSettings.newBuilder()
            .setDynamicColor(false)
            .setLockTimeoutMs(45_000)
            .putCardStyleByEntryType(-1, "default")
            .addVisibleVaultTab("login")
            .setVisibleVaultTabsConfigured(true)
            .putRuntimeExtra("show_access_history", "false")
            .setShowGeneralMessages(false)
            .setShowIconDownloadMessages(false)
            .setShowClipboardClearMessages(false)
            .setShowAppCloseMessages(false)
            .build()
        val output = ByteArrayOutputStream()

        AppSettingsSerializer.writeTo(settings, output)
        val decoded = AppSettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(settings, decoded)
        assertEquals(false, decoded.dynamicColor)
        assertEquals(true, decoded.hasDynamicColor())
        assertFalse(decoded.showGeneralMessages)
        assertEquals(true, decoded.hasShowGeneralMessages())
        assertEquals(true, decoded.hasShowIconDownloadMessages())
        assertEquals(true, decoded.hasShowClipboardClearMessages())
        assertEquals(true, decoded.hasShowAppCloseMessages())
    }

    @Test
    fun appSettings_rejectsCorruptProto() {
        assertThrows(CorruptionException::class.java) {
            runBlocking {
                AppSettingsSerializer.readFrom(ByteArrayInputStream(byteArrayOf(0x0A, 0x7F)))
            }
        }
    }
}

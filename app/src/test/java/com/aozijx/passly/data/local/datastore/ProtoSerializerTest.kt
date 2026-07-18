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
        assertEquals(true, defaults.statusBarNotificationsEnabled)
        assertFalse(defaults.hasStatusBarNotificationsEnabled())
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
            .setStatusBarNotificationsEnabled(false)
            .setIconDownloadNotificationsEnabled(false)
            .setClipboardClearToastsEnabled(false)
            .setAppCloseToastsEnabled(false)
            .build()
        val output = ByteArrayOutputStream()

        AppSettingsSerializer.writeTo(settings, output)
        val decoded = AppSettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(settings, decoded)
        assertEquals(false, decoded.dynamicColor)
        assertEquals(true, decoded.hasDynamicColor())
        assertFalse(decoded.statusBarNotificationsEnabled)
        assertEquals(true, decoded.hasStatusBarNotificationsEnabled())
        assertEquals(true, decoded.hasIconDownloadNotificationsEnabled())
        assertEquals(true, decoded.hasClipboardClearToastsEnabled())
        assertEquals(true, decoded.hasAppCloseToastsEnabled())
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

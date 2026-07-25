package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.data.local.datastore.settings.AppSettings
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.NoticeTopicProto
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        assertFalse(defaults.hasMessagePreferences())
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
            .setMessagePreferences(
                MessagePreferences.newBuilder()
                    .setOptionalMessagesEnabled(false)
                    .setSystemNotificationsEnabled(false)
                    .addTopics(
                        TopicMessagePreference.newBuilder()
                            .setTopic(NoticeTopicProto.NOTICE_TOPIC_BACKUP)
                            .setEnabled(false)
                            .setMinimumLevel(NoticeLevelProto.NOTICE_LEVEL_ERROR)
                    )
            )
            .build()
        val output = ByteArrayOutputStream()

        AppSettingsSerializer.writeTo(settings, output)
        val decoded = AppSettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(settings, decoded)
        assertEquals(false, decoded.dynamicColor)
        assertEquals(true, decoded.hasDynamicColor())
        assertTrue(decoded.hasMessagePreferences())
        assertFalse(decoded.messagePreferences.optionalMessagesEnabled)
        assertFalse(decoded.messagePreferences.systemNotificationsEnabled)
        assertEquals(1, decoded.messagePreferences.topicsCount)
        assertEquals(
            NoticeTopicProto.NOTICE_TOPIC_BACKUP,
            decoded.messagePreferences.getTopics(0).topic
        )
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

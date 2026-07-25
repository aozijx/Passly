package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.data.local.datastore.settings.AppSettings
import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.NoticeTopicProto
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.data.local.datastore.settings.SwipeActionProto
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.data.local.datastore.settings.VisibleTabs
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

        assertEquals(60_000L, defaults.security.lockTimeoutMs)
        assertEquals(true, defaults.appearance.dynamicColorEnabled)
        assertEquals(true, defaults.security.secureContentEnabled)
        assertEquals(false, defaults.interaction.swipeActionsEnabled)
        assertEquals(
            SwipeActionProto.SWIPE_ACTION_COPY_PASSWORD,
            defaults.interaction.swipeLeftAction
        )
        assertEquals(
            SwipeActionProto.SWIPE_ACTION_OPEN_DETAILS,
            defaults.interaction.swipeRightAction
        )
        assertEquals(4, defaults.vaultView.maxTabsWithoutScroll)
        assertFalse(defaults.hasMessage())
    }

    @Test
    fun appSettings_roundTripPreservesPresenceAndCollections() = runBlocking {
        val settings = AppSettings.newBuilder()
            .setAppearance(
                AppearancePreferences.newBuilder()
                    .setDynamicColorEnabled(false)
                    .build()
            )
            .setSecurity(
                SecurityPreferences.newBuilder()
                    .setLockTimeoutMs(45_000)
                    .build()
            )
            .setVaultView(
                VaultViewPreferences.newBuilder()
                    .setVisibleTabs(
                        VisibleTabs.newBuilder()
                            .addTabKeys("login")
                            .setConfigured(true)
                            .build()
                    )
                    .build()
            )
            .setMessage(
                MessagePreferences.newBuilder()
                    .setOptionalMessagesEnabled(false)
                    .setSystemNotificationsEnabled(false)
                    .addTopics(
                        TopicMessagePreference.newBuilder()
                            .setTopic(NoticeTopicProto.NOTICE_TOPIC_BACKUP)
                            .setEnabled(false)
                            .setMinimumLevel(NoticeLevelProto.NOTICE_LEVEL_ERROR)
                            .build()
                    )
                    .build()
            )
            .build()
        val output = ByteArrayOutputStream()

        AppSettingsSerializer.writeTo(settings, output)
        val decoded = AppSettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(settings, decoded)
        assertEquals(false, decoded.appearance.dynamicColorEnabled)
        assertTrue(decoded.hasAppearance())
        assertTrue(decoded.appearance.hasDynamicColorEnabled())
        assertTrue(decoded.hasMessage())
        assertFalse(decoded.message.optionalMessagesEnabled)
        assertFalse(decoded.message.systemNotificationsEnabled)
        assertEquals(1, decoded.message.topicsCount)
        assertEquals(
            NoticeTopicProto.NOTICE_TOPIC_BACKUP,
            decoded.message.getTopics(0).topic
        )
    }

    @Test
    fun appSettings_rejectsCorruptProto() {
        assertThrows(CorruptionException::class.java) {
            runBlocking {
                AppSettingsSerializer.readFrom(
                    ByteArrayInputStream(byteArrayOf(0x0A, 0x7F))
                )
            }
        }
    }
}

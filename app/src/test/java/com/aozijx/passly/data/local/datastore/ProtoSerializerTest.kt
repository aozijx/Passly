package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.data.local.datastore.settings.AppSettings
import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
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
    fun interactionSettings_fieldsAreFreshAndSequential() {
        assertEquals(
            (1..6).toList(),
            listOf(
                InteractionPreferences.SWIPE_ACTIONS_ENABLED_FIELD_NUMBER,
                InteractionPreferences.SWIPE_LEFT_ACTION_FIELD_NUMBER,
                InteractionPreferences.SWIPE_RIGHT_ACTION_FIELD_NUMBER,
                InteractionPreferences.AUTOFILL_FIELD_NUMBER,
                InteractionPreferences.AUTO_DOWNLOAD_ICONS_FIELD_NUMBER,
                InteractionPreferences.FAVICON_ALLOWED_DOMAINS_FIELD_NUMBER,
            ),
        )
    }

    @Test
    fun appSettings_schemaOwnsNonZeroDefaults() {
        val defaults = AppSettingsSerializer.defaultValue

        assertEquals(2, defaults.version)
        assertEquals(60_000L, defaults.security.lockTimeoutMs)
        assertTrue(defaults.security.reauthenticateSensitiveCopies)
        assertEquals(true, defaults.appearance.dynamicColorEnabled)
        assertEquals(true, defaults.security.secureContentEnabled)
        assertEquals(false, defaults.interaction.swipeActionsEnabled)
        assertEquals("copy_password", defaults.interaction.swipeLeftAction)
        assertEquals("detail", defaults.interaction.swipeRightAction)
        assertTrue(defaults.interaction.autofill.enabled)
        assertEquals("system_inline", defaults.interaction.autofill.presentation)
        assertTrue(defaults.interaction.autofill.credentialManagerEnabled)
        assertTrue(defaults.interaction.autofill.requireAuthentication)
        assertFalse(defaults.interaction.autofill.allowUnmatchedSuggestions)
        assertEquals(5, defaults.interaction.autofill.maxSuggestions)
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
                            .setTopicKey("backup")
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
        assertEquals("backup", decoded.message.getTopics(0).topicKey)
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

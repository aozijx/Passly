package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.data.preferences.proto.AppPreferences
import com.aozijx.passly.data.preferences.proto.AppearancePreferences
import com.aozijx.passly.data.preferences.proto.AutofillPresentationPreference
import com.aozijx.passly.data.preferences.proto.EntryHierarchyDisplayModePreference
import com.aozijx.passly.data.preferences.proto.MessageLevelPreference
import com.aozijx.passly.data.preferences.proto.SecurityPreferences
import com.aozijx.passly.data.preferences.proto.SortDirectionPreference
import com.aozijx.passly.data.preferences.proto.SwipeActionPreference
import com.aozijx.passly.data.preferences.proto.VaultSortFieldPreference
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
    fun appPreferences_defaultInstanceOwnsPersistedBehavior() {
        val defaults = AppPreferences.getDefaultInstance()

        assertTrue(defaults.appearance.dynamicColorEnabled)
        assertFalse(defaults.interaction.swipeActionsEnabled)
        assertFalse(defaults.interaction.pullToSearchEnabled)
        assertEquals(
            SwipeActionPreference.SWIPE_ACTION_COPY_PASSWORD,
            defaults.interaction.swipeLeftAction,
        )
        assertEquals(SwipeActionPreference.SWIPE_ACTION_DELETE, defaults.interaction.swipeRightAction)
        assertTrue(defaults.interaction.autofill.enabled)
        assertEquals(
            AutofillPresentationPreference.AUTOFILL_PRESENTATION_SYSTEM_INLINE,
            defaults.interaction.autofill.presentation,
        )
        assertEquals(
            VaultSortFieldPreference.VAULT_SORT_FIELD_CREATED_AT,
            defaults.vaultView.sort.field,
        )
        assertEquals(
            SortDirectionPreference.SORT_DIRECTION_DESCENDING,
            defaults.vaultView.sort.direction,
        )
        assertEquals(
            EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_COLLAPSED,
            defaults.vaultView.hierarchyDisplayMode,
        )
        assertEquals(
            MessageLevelPreference.MESSAGE_LEVEL_WARNING,
            defaults.message.securityMinimumLevel,
        )
        assertEquals(
            MessageLevelPreference.MESSAGE_LEVEL_ERROR,
            defaults.message.databaseMinimumLevel,
        )
    }

    @Test
    fun securityPreferences_defaultInstanceOwnsLockAndClipboardPolicy() {
        val defaults = SecurityPreferences.getDefaultInstance()

        assertTrue(defaults.secureContentEnabled)
        assertTrue(defaults.flipToLockEnabled)
        assertFalse(defaults.flipExitAndClearStack)
        assertFalse(defaults.lockOnBackground)
        assertEquals(60_000L, defaults.lockTimeoutMs)
        assertTrue(defaults.invalidateBiometricKeyOnChange)
        assertTrue(defaults.reauthenticateSensitiveCopies)
        assertTrue(defaults.clipboardClearEnabled)
        assertEquals(30, defaults.clipboardClearDelaySeconds)
    }

    @Test
    fun appPreferences_roundTripPreservesAppValues() = runBlocking {
        val preferences = AppPreferences.newBuilder()
            .setAppearance(
                AppearancePreferences.newBuilder()
                    .setDynamicColorEnabled(false)
                    .setThemeKey("oled")
                    .build(),
            )
            .build()
        val output = ByteArrayOutputStream()

        AppPreferencesSerializer.writeTo(preferences, output)
        val decoded = AppPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(preferences, decoded)
        assertFalse(decoded.appearance.dynamicColorEnabled)
        assertEquals("oled", decoded.appearance.themeKey)
    }

    @Test
    fun securityPreferences_roundTripPreservesSecurityValues() = runBlocking {
        val preferences = SecurityPreferences.newBuilder()
            .setLockOnBackground(true)
            .setLockTimeoutMs(45_000L)
            .setClipboardClearDelaySeconds(15)
            .build()
        val output = ByteArrayOutputStream()

        SecurityPreferencesSerializer.writeTo(preferences, output)
        val decoded = SecurityPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(preferences, decoded)
        assertTrue(decoded.lockOnBackground)
        assertEquals(45_000L, decoded.lockTimeoutMs)
        assertEquals(15, decoded.clipboardClearDelaySeconds)
    }

    @Test
    fun preferenceSerializers_rejectCorruptProto() {
        listOf<suspend () -> Unit>(
            { AppPreferencesSerializer.readFrom(ByteArrayInputStream(byteArrayOf(0x00))) },
            { SecurityPreferencesSerializer.readFrom(ByteArrayInputStream(byteArrayOf(0x00))) },
        ).forEach { readCorruptInput ->
            assertThrows(CorruptionException::class.java) {
                runBlocking { readCorruptInput() }
            }
        }
    }
}

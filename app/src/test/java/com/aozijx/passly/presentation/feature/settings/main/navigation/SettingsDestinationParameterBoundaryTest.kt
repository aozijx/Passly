package com.aozijx.passly.presentation.feature.settings.main.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsDestinationParameterBoundaryTest {
    @Test
    fun `leaf settings routes accept only state and events they consume`() {
        assertNarrow(
            "core/AppearanceRoute.kt",
            forbidden = listOf("SettingsViewModel"),
        )
        assertNarrow(
            "core/PrivacyRoute.kt",
            forbidden = listOf("SettingsViewModel"),
        )
        assertNarrow(
            "autofill/AutofillRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "SettingsOverlayState",
                "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                "SettingsViewModel", "SettingsUiState",
            ),
        )
        assertNarrow(
            "data/DataManagementRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "SettingsOverlayState",
                "InteractionSettingsViewModel",
            ),
        )
        assertNarrow(
            "interaction/InteractionRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "DataManagementSettingsViewModel",
                "InteractionSettingsViewModel", "SettingsViewModel", "SettingsUiState",
            ),
        )
        assertNarrow(
            "data/BackupRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "InteractionSettingsViewModel",
                "SettingsViewModel", "SettingsUiState",
            ),
        )
        assertNarrow(
            "core/RecoveryCodeRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "InteractionSettingsViewModel",
                "DataManagementSettingsViewModel", "SettingsUiState",
            ),
        )
        listOf("general/GeneralRoute.kt", "general/NotificationsRoute.kt").forEach { path ->
            assertNarrow(
                path,
                forbidden = listOf(
                    "SettingsDestination", "Context", "SettingsOverlayState",
                    "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                    "SettingsViewModel", "SettingsUiState",
                ),
            )
        }
    }

    @Test
    fun `root settings route does not own backup view model`() {
        assertFalse(
            "SettingsRoute still owns backup destination state",
            source("SettingsRoute.kt").contains("DataManagementSettingsViewModel"),
        )
    }
    private fun assertNarrow(relativePath: String, forbidden: List<String>) {
        val signature = source(relativePath)
            .substringAfter("internal fun ")
            .substringBefore(") {")
        forbidden.forEach { type ->
            assertFalse("$relativePath still accepts $type", Regex("\\b${Regex.escape(type)}\\b").containsMatchIn(signature))
        }
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/navigation/$relativePath",
        ).readText()
    }
}

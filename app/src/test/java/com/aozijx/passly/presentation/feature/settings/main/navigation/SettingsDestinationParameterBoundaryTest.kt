package com.aozijx.passly.presentation.feature.settings.main.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsDestinationParameterBoundaryTest {
    @Test
    fun `leaf settings routes accept only state and events they consume`() {
        assertNarrow(
            "core/SecurityRoute.kt",
            forbidden = listOf("AppPasswordSettingsViewModel"),
        )
        assertNarrow(
            "core/AppearanceRoute.kt",
            forbidden = listOf("AppPasswordSettingsViewModel"),
        )
        assertNarrow(
            "core/PrivacyRoute.kt",
            forbidden = listOf("AppPasswordSettingsViewModel"),
        )
        assertNarrow(
            "autofill/AutofillRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "AppPasswordDialogStateHolder",
                "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                "AppPasswordSettingsViewModel", "AppPasswordSettingsUiState",
            ),
        )
        assertNarrow(
            "data/DataManagementRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "AppPasswordDialogStateHolder",
                "InteractionSettingsViewModel",
            ),
        )
        assertNarrow(
            "interaction/InteractionRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "AppPasswordDialogStateHolder",
                "DataManagementSettingsViewModel",
                "InteractionSettingsViewModel", "AppPasswordSettingsViewModel", "AppPasswordSettingsUiState",
            ),
        )
        assertNarrow(
            "data/BackupRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "InteractionSettingsViewModel",
                "AppPasswordSettingsViewModel", "AppPasswordSettingsUiState",
            ),
        )
        assertNarrow(
            "core/RecoveryCodeRoute.kt",
            forbidden = listOf(
                "SettingsDestination", "Context", "AppPasswordDialogStateHolder",
                "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                "AppPasswordSettingsUiState",
            ),
        )
        listOf("general/GeneralRoute.kt", "general/NotificationsRoute.kt").forEach { path ->
            assertNarrow(
                path,
                forbidden = listOf(
                    "SettingsDestination", "Context", "AppPasswordDialogStateHolder",
                    "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                    "AppPasswordSettingsViewModel", "AppPasswordSettingsUiState",
                ),
            )
        }
    }

    @Test
    fun `settings graph delegates view model ownership to route`() {
        val graph = source("SettingsGraphRegistration.kt")
        assertFalse(graph.contains("AppPasswordSettingsViewModel"))
        assertFalse(graph.contains("hiltViewModel"))
    }
    @Test
    fun `root settings route does not own destination view models`() {
        val route = source("SettingsRoute.kt")
        assertFalse(route.contains("AppPasswordSettingsViewModel"))
        assertFalse(route.contains("hiltViewModel"))
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

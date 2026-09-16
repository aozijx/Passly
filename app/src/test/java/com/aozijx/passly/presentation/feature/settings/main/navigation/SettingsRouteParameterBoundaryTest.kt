package com.aozijx.passly.presentation.feature.settings.main.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsRouteParameterBoundaryTest {
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
                "SettingsRoute", "Context", "SettingsScreenLocalState",
                "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                "SettingsViewModel", "SettingsUiState",
            ),
        )
        assertNarrow(
            "data/DataManagementRoute.kt",
            forbidden = listOf("SettingsRoute", "Context", "InteractionSettingsViewModel"),
        )
        listOf("general/GeneralRoute.kt", "general/NotificationsRoute.kt").forEach { path ->
            assertNarrow(
                path,
                forbidden = listOf(
                    "SettingsRoute", "Context", "SettingsScreenLocalState",
                    "InteractionSettingsViewModel", "DataManagementSettingsViewModel",
                    "SettingsViewModel", "SettingsUiState",
                ),
            )
        }
    }

    private fun assertNarrow(relativePath: String, forbidden: List<String>) {
        val signature = source(relativePath)
            .substringAfter("internal fun ")
            .substringBefore(") {")
        forbidden.forEach { type ->
            assertFalse("$relativePath still accepts $type", signature.contains(type))
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
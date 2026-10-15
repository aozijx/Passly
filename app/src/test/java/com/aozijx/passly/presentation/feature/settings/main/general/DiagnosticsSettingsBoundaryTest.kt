package com.aozijx.passly.presentation.feature.settings.main.general

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsSettingsBoundaryTest {
    @Test
    fun `diagnostics settings depends on a feature log store port`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val viewModel = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/general/DiagnosticsSettingsViewModel.kt",
        ).readText()

        assertFalse(viewModel.contains("app.diagnostics.DiagnosticsRuntimeController"))
        assertTrue(viewModel.contains("DiagnosticsLogStore"))
        assertFalse(viewModel.contains("Dispatchers.IO"))
        assertFalse(viewModel.contains("withContext"))
    }

    @Test
    fun `general page requests a diagnostics cache refresh on entry`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val contract = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/general/DiagnosticsSettingsContract.kt",
        ).readText()
        val route = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/navigation/general/GeneralRoute.kt",
        ).readText()

        assertTrue(contract.contains("data object RefreshLogs"))
        assertTrue(route.contains("DiagnosticsSettingsAction.RefreshLogs"))
    }
}

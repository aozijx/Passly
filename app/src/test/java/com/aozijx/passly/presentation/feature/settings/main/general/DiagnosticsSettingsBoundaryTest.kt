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
}

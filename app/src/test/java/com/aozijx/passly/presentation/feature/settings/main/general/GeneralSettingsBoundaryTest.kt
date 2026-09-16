package com.aozijx.passly.presentation.feature.settings.main.general

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneralSettingsBoundaryTest {
    @Test
    fun `general settings depends on a feature cache store port`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val viewModel = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/general/GeneralSettingsViewModel.kt",
        ).readText()

        assertFalse(viewModel.contains("app.cache.AppCacheManager"))
        assertFalse(viewModel.contains("AppCacheManager"))
        assertTrue(viewModel.contains("AppCacheStore"))
        assertTrue(viewModel.contains("ByteSizeFormatter"))
    }
}
package com.aozijx.passly.presentation.feature.settings.autofill

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSettingsOwnershipBoundaryTest {
    @Test
    fun `autofill settings use one typed event sink without handler bridge`() {
        val sourceRoot = sourceRoot()
        val settingsRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings",
        )
        val route = settingsRoot.resolve("main/navigation/autofill/AutofillRoute.kt").readText()
        val models = settingsRoot.resolve("ui/autofill/model/AutofillSettingsUiModel.kt").readText()
        val detail = settingsRoot.resolve("ui/autofill/AutofillDetail.kt").readText()
        val section = settingsRoot.resolve("ui/autofill/AutofillSettingsSection.kt").readText()

        assertFalse(models.contains("AutofillSettingsEventHandler"))
        assertTrue(models.contains("sealed interface AutofillSettingsEvent"))
        assertTrue(detail.contains("onEvent: (AutofillSettingsEvent) -> Unit"))
        assertTrue(section.contains("onEvent: (AutofillSettingsEvent) -> Unit"))
        assertFalse(route.contains("object : AutofillSettingsEventHandler"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}

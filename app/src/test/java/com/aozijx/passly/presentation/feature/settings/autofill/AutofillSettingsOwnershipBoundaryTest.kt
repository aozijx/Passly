package com.aozijx.passly.presentation.feature.settings.autofill

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSettingsOwnershipBoundaryTest {
    @Test
    fun `autofill settings send view model actions without a duplicate ui event contract`() {
        val sourceRoot = sourceRoot()
        val settingsRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings",
        )
        val platformRoot = sourceRoot.resolve("com/aozijx/passly/app/autofill")
        val route = settingsRoot.resolve("main/navigation/autofill/AutofillRoute.kt").readText()
        val contract = settingsRoot.resolve("autofill/AutofillSettingsContract.kt").readText()
        val viewModel = settingsRoot.resolve("autofill/AutofillSettingsViewModel.kt").readText()
        val models = settingsRoot.resolve("ui/autofill/model/AutofillSettingsUiModel.kt").readText()
        val detail = settingsRoot.resolve("ui/autofill/AutofillDetail.kt").readText()
        val section = settingsRoot.resolve("ui/autofill/AutofillSettingsSection.kt").readText()

        assertFalse(models.contains("AutofillSettingsEvent"))
        assertTrue(contract.contains("enum class AutofillPresentationUiModel"))
        assertFalse(models.contains("enum class AutofillPresentationUiModel"))
        assertTrue(detail.contains("onAction: (AutofillSettingsAction) -> Unit"))
        assertTrue(section.contains("onAction: (AutofillSettingsAction) -> Unit"))
        assertTrue(route.contains("onAction = viewModel::onAction"))
        assertFalse(route.contains("when (event)"))
        assertTrue(contract.contains("data object OpenSystemAutofillSettings : AutofillSettingsEffect"))
        assertTrue(route.contains("viewModel.effects.collect"))
        assertFalse(viewModel.contains("autofillPlatformGateway.openSystemSettings()"))
        assertTrue(viewModel.contains("AutofillServiceStatusSource"))
        assertFalse(viewModel.contains("AutofillPlatformGateway"))
        assertTrue(platformRoot.resolve("AutofillPlatformModule.kt").isFile)
        assertFalse(platformRoot.resolve("AutofillLaunchTargetModule.kt").exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}

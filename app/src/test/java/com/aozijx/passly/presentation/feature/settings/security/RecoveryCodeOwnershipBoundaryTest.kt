package com.aozijx.passly.presentation.feature.settings.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeOwnershipBoundaryTest {
    @Test
    fun `route connects one page state owner without revealing credentials`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/core/RecoveryCodeRoute.kt",
        ).readText()
        val viewModel = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/security/RecoveryCodeSettingsViewModel.kt",
        )

        assertTrue(viewModel.exists())
        assertTrue(route.contains("RecoveryCodeSettingsViewModel"))
        assertFalse(route.contains("SecuritySettingsViewModel"))
        assertFalse(route.contains("RecoveryDraftViewModel"))
        assertFalse(route.contains("revealCode"))
        assertFalse(route.contains("showRecoveryCodeSheet"))
        assertFalse(route.contains("LaunchedEffect(recoveryCode)"))
    }
}

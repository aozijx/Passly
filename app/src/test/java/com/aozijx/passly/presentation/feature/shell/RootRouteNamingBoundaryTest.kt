package com.aozijx.passly.presentation.feature.shell

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootRouteNamingBoundaryTest {
    @Test
    fun `unlock and recovery entry points separate route ownership from passive screens`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")

        val unlockRoute = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/unlock/AuthenticationRoute.kt",
        )
        val unlockScreen = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/unlock/ui/AuthenticationScreen.kt",
        )
        val recoveryRoute = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/recovery/RecoveryModeRoute.kt",
        )
        val recoveryScreen = sourceRoot.resolve(
            "com/aozijx/passly/presentation/ui/recovery/RecoveryModeScreen.kt",
        )

        listOf(unlockRoute, unlockScreen, recoveryRoute, recoveryScreen).forEach { file ->
            assertTrue("Missing semantic route/screen file: ${file.path}", file.isFile)
        }
        assertTrue(unlockRoute.readText().contains("fun AuthenticationRoute("))
        assertTrue(unlockScreen.readText().contains("fun AuthenticationScreen("))
        assertTrue(recoveryRoute.readText().contains("fun RecoveryModeRoute("))
        assertTrue(recoveryScreen.readText().contains("fun RecoveryModeScreen("))
        assertFalse(unlockScreen.readText().contains("ViewModel"))
        assertFalse(recoveryScreen.readText().contains("ViewModel"))

        val obsoleteOnboarding = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/onboarding",
        )
        assertFalse(
            "Unlock password provisioning must not be owned by a second feature state machine",
            obsoleteOnboarding.walkTopDown().any { it.isFile && it.extension == "kt" },
        )
        assertFalse(unlockRoute.readText().contains("BootstrapViewModel"))
    }
}

package com.aozijx.passly.presentation.feature.unlock

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationRouteBoundaryTest {
    @Test
    fun `route passes the authoritative state and action stream without ui bridges`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/unlock/AuthenticationRoute.kt",
        ).readText()
        val screen = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/unlock/ui/AuthenticationScreen.kt",
        ).readText()

        assertFalse(route.contains("AuthenticationScreenState("))
        assertFalse(route.contains("AuthenticationScreenEvents("))
        assertFalse(route.contains("toUiMethod"))
        assertFalse(route.contains("AppPasswordPolicy"))
        assertFalse(route.contains("AuthenticationFailure"))
        assertTrue(route.contains("state = state"))
        assertTrue(route.contains("onAction = viewModel::onAction"))
        assertFalse(screen.contains("class AuthenticationScreenState"))
        assertFalse(screen.contains("class AuthenticationScreenEvents"))
        assertFalse(screen.contains("enum class AuthenticationInputMethod"))
        assertFalse(screen.contains("AuthenticationFailure"))
        assertFalse(screen.contains("AppPasswordPolicy"))
    }
}

package com.aozijx.passly.presentation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasslyAppBoundaryTest {

    @Test
    fun `activity delegates compose root assembly to PasslyApp`() {
        val activity = source("com/aozijx/passly/MainActivity.kt")

        assertTrue(activity.contains("PasslyApp("))
        listOf(
            "ProvidePermissionServices",
            "ProvideAppNoticePublisher",
            "AppTheme(",
            "AuthenticationHost(",
        ).forEach { token -> assertFalse(activity.contains(token)) }
    }

    @Test
    fun `PasslyApp owns global compose providers and authentication host`() {
        val app = source("com/aozijx/passly/presentation/PasslyApp.kt")

        listOf(
            "ProvidePermissionServices",
            "ProvideAppNoticePublisher",
            "AppTheme(",
            "AuthenticationHost(",
            "AppShell(",
        ).forEach { token -> assertTrue(app.contains(token)) }
    }

    @Test
    fun `navigation context contains navigation only`() {
        val navigationContext = source(
            "com/aozijx/passly/presentation/feature/shell/navigation/ShellNavigationContext.kt",
        )
        val detailList = source(
            "com/aozijx/passly/presentation/feature/vault/detail/ui/component/DetailScrollableContent.kt",
        )

        assertFalse(navigationContext.contains("onUserInteraction"))
        assertFalse(detailList.contains("clickable("))
    }
    @Test
    fun `shell depends on window and close intent instead of activity`() {
        val shell = source(
            "com/aozijx/passly/presentation/feature/shell/AppShell.kt",
        )

        assertFalse(shell.contains("FragmentActivity"))
        assertFalse(shell.contains("finishAffinity"))
        assertFalse(shell.contains("AppShellViewModel"))
        val navigation = source(
            "com/aozijx/passly/presentation/feature/shell/PasslyAppNavigation.kt",
        )
        assertFalse(navigation.contains("AppShellViewModel"))
        assertTrue(shell.contains("window: Window"))
        assertTrue(shell.contains("onCloseApp: () -> Unit"))
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}

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
        assertFalse(activity.contains("Intent.ACTION_SCREEN_OFF"))
        assertFalse(activity.contains("AppShellViewModel"))
        assertFalse(activity.contains("AppShellUiAction"))
        assertFalse(activity.contains("SensorManager"))
        assertTrue(activity.contains("deviceLockController.clearTaskRequests"))
    }

    @Test
    fun `PasslyApp owns global compose providers and authentication host`() {
        val app = source("com/aozijx/passly/presentation/PasslyApp.kt")

        listOf(
            "ProvidePermissionServices",
            "ProvideAppNoticePublisher",
            "AppTheme(",
            "AuthenticationHost(",
            "AppNoticeEffects(",
            "AppShell(",
        ).forEach { token -> assertTrue(app.contains(token)) }
        assertFalse(app.contains("DeviceLockController"))
        assertTrue(app.contains("hiltViewModel<AppShellViewModel>()"))
    }

    @Test
    fun `screen off locking is process scoped instead of activity scoped`() {
        val application = source("com/aozijx/passly/app/PasslyApplication.kt")
        val controller = source("com/aozijx/passly/app/DeviceLockController.kt")

        assertTrue(application.contains("deviceLockController.start()"))
        assertTrue(controller.contains("Intent.ACTION_SCREEN_OFF"))
        assertTrue(controller.contains("ContextCompat.RECEIVER_EXPORTED"))
        assertTrue(controller.contains("goAsync()"))
        assertTrue(controller.contains("DeviceLockTrigger.FLIP"))
        assertTrue(controller.contains("screenOffLockTracker.markScreenOff()"))
        assertTrue(controller.contains("screenOffLockTracker.pendingGeneration()"))
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
    fun `shell projects session and database state without duplicate owners`() {
        val uiState = source(
            "com/aozijx/passly/presentation/feature/shell/AppShellUiState.kt",
        )
        val reducer = source(
            "com/aozijx/passly/presentation/feature/shell/AppShellReducer.kt",
        )
        val viewModel = source(
            "com/aozijx/passly/presentation/feature/shell/AppShellViewModel.kt",
        )

        assertTrue(uiState.contains("sessionMode: AppShellSessionMode"))
        assertFalse(uiState.contains("isAuthorized"))
        assertFalse(uiState.contains("isRecoveryMode"))
        assertFalse(reducer.contains("data object Authenticated"))
        assertFalse(reducer.contains("data object SessionLocked"))
        assertTrue(reducer.contains("DatabaseFailureChanged(val error: Throwable?)"))
        assertTrue(viewModel.contains("state.toAppShellSessionMode()"))
        assertTrue(viewModel.contains("DatabaseFailureChanged(error)"))
        assertFalse(viewModel.contains("if (error != null)"))
        assertTrue(viewModel.contains("_uiState.update"))
    }

    @Test
    fun `shell owns root rendering while notice presentation owns its effect host`() {
        val shell = source(
            "com/aozijx/passly/presentation/feature/shell/AppShell.kt",
        )
        val noticeEffects = source(
            "com/aozijx/passly/app/message/presentation/AppNoticeEffects.kt",
        )

        assertFalse(shell.contains("FragmentActivity"))
        assertFalse(shell.contains("finishAffinity"))
        assertFalse(shell.contains("AppShellViewModel"))
        assertFalse(shell.contains("DeviceLockController"))
        assertFalse(shell.contains("AppShellSettingsViewModel"))
        assertFalse(shell.contains("AppNoticeHostViewModel"))
        assertFalse(shell.contains("toastMessages"))
        val navigation = source(
            "com/aozijx/passly/presentation/feature/shell/PasslyAppNavigation.kt",
        )
        assertFalse(navigation.contains("AppShellViewModel"))
        assertTrue(shell.contains("window: Window"))
        assertTrue(shell.contains("onCloseApp: () -> Unit"))
        assertTrue(shell.contains("fun AppShell("))
        assertTrue(noticeEffects.contains("hiltViewModel<AppNoticeHostViewModel>()"))
        assertTrue(noticeEffects.contains("toastMessages.collect"))

        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        assertFalse(
            sourceRoot.resolve("com/aozijx/passly/presentation/feature/shell/AppShellRoute.kt").exists(),
        )
        assertFalse(
            sourceRoot.resolve("com/aozijx/passly/presentation/ui/shell")
                .walkTopDown().any { it.isFile && it.extension == "kt" },
        )
        assertTrue(
            sourceRoot.resolve(
                "com/aozijx/passly/presentation/feature/shell/ui/DatabaseErrorDialog.kt",
            ).isFile,
        )
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}

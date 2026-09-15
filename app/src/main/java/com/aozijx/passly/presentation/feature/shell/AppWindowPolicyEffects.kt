package com.aozijx.passly.presentation.feature.shell

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aozijx.passly.app.shell.FlipToLockSensorController

@Composable
internal fun AppWindowPolicyEffects(
    window: Window,
    settings: AppShellSettingsUiState,
    sensorController: FlipToLockSensorController,
) {
    SideEffect {
        if (settings.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        sensorController.isFlipExitAndClearStackEnabled =
            settings.isFlipExitAndClearStackEnabled

        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            if (settings.isStatusBarAutoHide) {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
    }

    DisposableEffect(sensorController, settings.isFlipToLockEnabled) {
        sensorController.isFlipLockEnabled = settings.isFlipToLockEnabled
        if (settings.isFlipToLockEnabled) {
            sensorController.register()
        } else {
            sensorController.unregister()
        }
        onDispose(sensorController::unregister)
    }
}

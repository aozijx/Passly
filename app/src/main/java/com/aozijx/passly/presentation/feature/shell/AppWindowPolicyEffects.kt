package com.aozijx.passly.presentation.feature.shell

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun AppWindowPolicyEffects(
    window: Window,
    policy: AppWindowPolicy,
) {
    SideEffect {
        if (policy.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            if (policy.isStatusBarAutoHide) {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
    }
}

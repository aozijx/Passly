package com.aozijx.passly.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

/**
 * 通知权限请求控制器。
 *
 * 封装通知权限的系统版本判断、授权状态检查与请求入口，
 * Activity 仅需在合适时机调用 [requestIfNeeded]。
 */
internal class MainNotificationPermissionController(
    private val activity: FragmentActivity,
    private val onDenied: () -> Unit
) {
    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) onDenied()
        }

    fun requestIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

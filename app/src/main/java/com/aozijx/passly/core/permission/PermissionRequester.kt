package com.aozijx.passly.core.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

interface PermissionRequester {
    fun snapshot(permission: AppPermission): PermissionSnapshot

    /**
     * 发起仍需申请的运行时权限。已有请求执行中时返回 false。
     * Manifest 权限及当前系统不适用的权限不会触发系统弹窗，但仍会出现在结果中。
     */
    fun request(vararg permissions: AppPermission): Boolean
}

class ActivityPermissionRequester(
    private val activity: ComponentActivity,
    private val manager: AppPermissionManager,
    private val onResult: (PermissionRequestResult) -> Unit
) : PermissionRequester {
    private var pendingPermissions = emptySet<AppPermission>()
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        deliverResult()
    }

    override fun snapshot(permission: AppPermission): PermissionSnapshot =
        manager.snapshot(permission, activity)

    override fun request(vararg permissions: AppPermission): Boolean {
        if (pendingPermissions.isNotEmpty()) return false
        val requested = permissions.toSet()
        require(requested.isNotEmpty()) { "At least one permission is required" }
        val androidNames = manager.requestableAndroidNames(requested)
        if (androidNames.isEmpty()) {
            onResult(PermissionRequestResult(manager.snapshots(requested, activity)))
            return false
        }

        pendingPermissions = requested
        launcher.launch(androidNames.toTypedArray())
        return true
    }

    private fun deliverResult() {
        val requested = pendingPermissions
        pendingPermissions = emptySet()
        onResult(PermissionRequestResult(manager.snapshots(requested, activity)))
    }
}

@Composable
fun rememberAppPermissionManager(): AppPermissionManager {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidAppPermissionManager(context) }
}

@Composable
fun rememberAppPermissionRequester(
    manager: AppPermissionManager = rememberAppPermissionManager(),
    onResult: (PermissionRequestResult) -> Unit
): PermissionRequester {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult = rememberUpdatedState(onResult)
    val pendingPermissions = remember { mutableStateOf(emptySet<AppPermission>()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val requested = pendingPermissions.value
        pendingPermissions.value = emptySet()
        currentOnResult.value(
            PermissionRequestResult(manager.snapshots(requested, activity))
        )
    }

    return remember(manager, activity, launcher) {
        object : PermissionRequester {
            override fun snapshot(permission: AppPermission): PermissionSnapshot =
                manager.snapshot(permission, activity)

            override fun request(vararg permissions: AppPermission): Boolean {
                if (pendingPermissions.value.isNotEmpty()) return false
                val requested = permissions.toSet()
                require(requested.isNotEmpty()) { "At least one permission is required" }
                val androidNames = manager.requestableAndroidNames(requested)
                if (androidNames.isEmpty()) {
                    currentOnResult.value(
                        PermissionRequestResult(manager.snapshots(requested, activity))
                    )
                    return false
                }

                pendingPermissions.value = requested
                launcher.launch(androidNames.toTypedArray())
                return true
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

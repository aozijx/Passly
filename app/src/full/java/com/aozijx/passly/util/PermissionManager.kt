package com.aozijx.passly.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.logging.Logcat
import java.lang.ref.WeakReference

/**
 * 权限管理类，支持 Lifecycle 感知，自动处理清理逻辑
 */
class PermissionManager private constructor() : DefaultLifecycleObserver {

    private var notificationLauncher: ActivityResultLauncher<String>? = null
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var onResult: ((PermissionResult) -> Unit)? = null

    sealed class PermissionResult {
        object Granted : PermissionResult()
        object Denied : PermissionResult()
        object PermanentlyDenied : PermissionResult()
    }

    companion object {
        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(): PermissionManager =
            instance ?: synchronized(this) {
                instance ?: PermissionManager().also { instance = it }
            }
    }

    /**
     * 初始化并绑定 Activity 生命周期
     * 必须在 Activity.onCreate 或之前调用
     */
    fun init(activity: ComponentActivity, onUpdate: (Boolean) -> Unit) {
        // 绑定生命周期，自动在 onDestroy 时清理
        activity.lifecycle.addObserver(this)
        
        activityRef = WeakReference(activity)
        notificationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onUpdate(isGranted)
            val currentActivity = activityRef?.get()
            val result = when {
                isGranted -> PermissionResult.Granted
                currentActivity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    currentActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> PermissionResult.PermanentlyDenied
                else -> PermissionResult.Denied
            }
            onResult?.invoke(result)
            onResult = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        unregister()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission(callback: ((PermissionResult) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onResult = callback
            val launcher = notificationLauncher
            if (launcher != null) {
                try {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    Logcat.e("PermissionManager", "权限请求启动失败", e)
                    onResult?.invoke(PermissionResult.Denied)
                    onResult = null
                }
            } else {
                onResult?.invoke(PermissionResult.Denied)
                onResult = null
            }
        } else {
            callback?.invoke(PermissionResult.Granted)
        }
    }
    
    private fun unregister() {
        notificationLauncher = null
        activityRef = null
        onResult = null
    }
}


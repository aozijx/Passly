package com.aozijx.passly.core.permission.catalog

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import com.aozijx.passly.core.permission.model.RuntimePermission

object RuntimePermissionCatalog {
    @SuppressLint("InlinedApi")
    fun androidName(
        permission: RuntimePermission,
        apiLevel: Int = Build.VERSION.SDK_INT
    ): String? = when (permission) {
        RuntimePermission.CAMERA -> Manifest.permission.CAMERA
        RuntimePermission.POST_NOTIFICATIONS ->
            Manifest.permission.POST_NOTIFICATIONS.takeIf {
                apiLevel >= Build.VERSION_CODES.TIRAMISU
            }
    }
}

enum class ManifestCapability {
    INTERNET,
    VIBRATE,
    USE_BIOMETRIC,
    PACKAGE_VISIBILITY
}

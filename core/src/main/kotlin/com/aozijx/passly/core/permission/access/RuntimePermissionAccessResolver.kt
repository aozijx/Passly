package com.aozijx.passly.core.permission.access

import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess

object RuntimePermissionAccessResolver {
    fun resolve(
        status: PermissionStatus,
        requestedBefore: Boolean,
        shouldShowRationale: Boolean
    ): RuntimePermissionAccess = when (status) {
        PermissionStatus.GRANTED -> RuntimePermissionAccess.GRANTED
        PermissionStatus.NOT_APPLICABLE -> RuntimePermissionAccess.NOT_APPLICABLE
        PermissionStatus.DENIED -> {
            if (!requestedBefore || shouldShowRationale) {
                RuntimePermissionAccess.REQUESTABLE
            } else {
                RuntimePermissionAccess.SETTINGS_REQUIRED
            }
        }
    }
}

package com.aozijx.passly.core.permission.contract

import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission

fun interface PermissionStatusReader {
    fun status(permission: RuntimePermission): PermissionStatus
}

interface PermissionRequestHistory {
    fun wasRequested(permission: RuntimePermission): Boolean
    fun markRequested(permission: RuntimePermission)
}

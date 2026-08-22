package com.aozijx.passly.core.permission.request

interface PermissionRequestArbiter {
    fun tryAcquire(owner: String): PermissionRequestLease?
}

interface PermissionRequestLease {
    fun release()
}

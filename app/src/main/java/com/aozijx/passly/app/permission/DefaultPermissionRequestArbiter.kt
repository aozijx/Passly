package com.aozijx.passly.app.permission

import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.core.permission.request.PermissionRequestLease
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPermissionRequestArbiter @Inject constructor() : PermissionRequestArbiter {
    private var activeOwner: String? = null

    @Synchronized
    override fun tryAcquire(owner: String): PermissionRequestLease? {
        require(owner.isNotBlank()) { "Permission request owner must not be blank" }
        if (activeOwner != null) return null
        activeOwner = owner
        var released = false
        return object : PermissionRequestLease {
            override fun release() {
                synchronized(this@DefaultPermissionRequestArbiter) {
                    if (released) return
                    released = true
                    if (activeOwner == owner) activeOwner = null
                }
            }
        }
    }
}

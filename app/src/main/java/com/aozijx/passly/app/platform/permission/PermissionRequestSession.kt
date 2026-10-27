package com.aozijx.passly.app.platform.permission

import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.core.permission.request.PermissionRequestLease

/** Main-thread owner of a host's request and process-wide dialog lease. */
internal class PermissionRequestSession(
    private val owner: String,
    private val arbiter: PermissionRequestArbiter,
    private val history: PermissionRequestHistory,
) : AutoCloseable {
    private sealed interface State {
        data object Idle : State
        class Pending(
            val permission: RuntimePermission,
            val androidName: String,
            val requestedBefore: Boolean,
            val lease: PermissionRequestLease,
        ) : State
        data object Closed : State
    }

    private var state: State = State.Idle
    val isClosed: Boolean get() = state == State.Closed
    val hasPendingRequest: Boolean get() = state is State.Pending

    fun request(
        permission: RuntimePermission,
        androidName: String,
        launch: (String) -> PermissionRequestStart,
    ): PermissionRequestStart {
        when (state) {
            State.Closed -> return PermissionRequestStart.HostUnavailable
            is State.Pending -> return PermissionRequestStart.Busy
            State.Idle -> Unit
        }
        val lease = arbiter.tryAcquire(owner) ?: return PermissionRequestStart.Busy
        val requestedBefore = try {
            history.wasRequested(permission).also { history.markRequested(permission) }
        } catch (error: Throwable) {
            lease.release()
            throw error
        }
        val pending = State.Pending(permission, androidName, requestedBefore, lease)
        state = pending
        try {
            val result = launch(androidName)
            if (result != PermissionRequestStart.Launched && state === pending) {
                state = State.Idle
                lease.release()
            }
            return if (isClosed) PermissionRequestStart.HostUnavailable else result
        } catch (error: Throwable) {
            if (state === pending) {
                state = State.Idle
                lease.release()
            }
            throw error
        }
    }

    fun complete(deliver: (RuntimePermission, String, Boolean) -> Unit) {
        val pending = state as? State.Pending ?: return
        state = State.Idle
        pending.lease.release()
        deliver(pending.permission, pending.androidName, pending.requestedBefore)
    }

    override fun close() {
        val previous = state
        state = State.Closed
        if (previous is State.Pending) previous.lease.release()
    }
}

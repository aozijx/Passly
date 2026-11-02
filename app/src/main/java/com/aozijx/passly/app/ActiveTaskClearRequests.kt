package com.aozijx.passly.app

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@JvmInline
internal value class TaskClearRequestId(
    val value: Long,
)

@JvmInline
internal value class TaskClearOwnerId(
    val value: Long = ownerIds.incrementAndGet(),
) {
    companion object {
        private val ownerIds = AtomicLong()
    }
}

internal data class TaskClearClaim(
    val requestId: TaskClearRequestId,
    val ownerId: TaskClearOwnerId,
)

/** Holds one process-local task-clear request until the current Activity consumes or releases it. */
@Singleton
class ActiveTaskClearRequests
    @Inject
    constructor(
        private val sessionAccessState: SecureSessionAccessState,
    ) {
        private val requestIds = AtomicLong()
        private var pendingRequest: TaskClearRequestId? = null
        private var currentOwner: Owner? = null
        private var currentClaim: TaskClearClaim? = null

        @Synchronized
        internal fun requestClear(): TaskClearRequestId? {
            if (sessionAccessState.authenticationState.value != AuthenticationState.Locked) return null
            cancelCurrentClaim()
            val requestId = TaskClearRequestId(requestIds.incrementAndGet())
            pendingRequest = requestId
            deliverPendingRequest()
            return requestId
        }

        @Synchronized
        internal fun registerOwner(
            ownerId: TaskClearOwnerId,
            onRequest: (TaskClearClaim) -> Unit,
            onCancelled: (TaskClearRequestId) -> Unit,
        ) {
            if (currentOwner?.id == ownerId) return
            cancelCurrentClaim()
            currentOwner = Owner(ownerId, onRequest, onCancelled)
            deliverPendingRequest()
        }

        @Synchronized
        internal fun releaseOwner(ownerId: TaskClearOwnerId) {
            if (currentOwner?.id != ownerId) return
            currentClaim = null
            currentOwner = null
        }

        @Synchronized
        internal fun isCurrent(claim: TaskClearClaim): Boolean =
            pendingRequest == claim.requestId &&
                currentOwner?.id == claim.ownerId &&
                currentClaim == claim &&
                sessionAccessState.authenticationState.value == AuthenticationState.Locked

        @Synchronized
        internal fun consume(claim: TaskClearClaim): Boolean {
            if (!isCurrent(claim)) return false
            pendingRequest = null
            currentClaim = null
            return true
        }

        @Synchronized
        internal fun onAuthenticationStateChanged(state: AuthenticationState) {
            when (state) {
                AuthenticationState.Locked -> {
                    val claim = currentClaim
                    val owner = currentOwner
                    if (claim != null && owner?.id == claim.ownerId) {
                        owner.onRequest(claim)
                    } else {
                        deliverPendingRequest()
                    }
                }

                is AuthenticationState.Authenticated,
                is AuthenticationState.AutofillAuthenticated,
                -> {
                    cancelCurrentClaim()
                    pendingRequest = null
                }

                else -> {
                    Unit
                }
            }
        }

        private fun deliverPendingRequest() {
            val requestId = pendingRequest ?: return
            val owner = currentOwner ?: return
            if (currentClaim != null) return
            val claim = TaskClearClaim(requestId, owner.id)
            currentClaim = claim
            owner.onRequest(claim)
        }

        private fun cancelCurrentClaim() {
            val claim = currentClaim ?: return
            currentOwner?.takeIf { it.id == claim.ownerId }?.onCancelled?.invoke(claim.requestId)
            currentClaim = null
        }

        private data class Owner(
            val id: TaskClearOwnerId,
            val onRequest: (TaskClearClaim) -> Unit,
            val onCancelled: (TaskClearRequestId) -> Unit,
        )
    }

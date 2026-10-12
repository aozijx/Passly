package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AuthorizeAutofillInteraction @Inject constructor(
    private val authenticationRequester: AuthenticationRequester,
    private val vaultAccessState: SecureSessionAccessState,
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
) {
    private data class Attempt(
        val result: CompletableDeferred<AutofillAuthorizationOutcome>,
        val owner: Boolean,
        val hadFullAccess: Boolean,
    )

    private val mutex = Mutex()
    private val attempts = mutableMapOf<AutofillInteractionId, CompletableDeferred<AutofillAuthorizationOutcome>>()

    suspend operator fun invoke(id: AutofillInteractionId): AutofillAuthorizationOutcome {
        val attempt = prepare(id) ?: return existingOutcome(id)
        if (!attempt.owner) return attempt.result.await()

        return try {
            val authentication = authenticationRequester.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL),
            )
            finish(id, attempt, authentication)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { cancelAttempt(id, attempt) }
            throw cancelled
        }
    }

    private suspend fun prepare(id: AutofillInteractionId): Attempt? = mutex.withLock {
        val snapshot = store.snapshot(id) ?: return null
        if (snapshot.state != AutofillInteractionState.RequiresUnlock) return null
        attempts[id]?.let { return Attempt(it, owner = false, hadFullAccess = false) }
        val deferred = CompletableDeferred<AutofillAuthorizationOutcome>()
        attempts[id] = deferred
        Attempt(deferred, owner = true, hadFullAccess = vaultAccessState.hasFullSecureSessionAccess())
    }

    private suspend fun existingOutcome(id: AutofillInteractionId): AutofillAuthorizationOutcome {
        val snapshot = store.snapshot(id)
            ?: return AutofillAuthorizationOutcome.Rejected(
                AutofillInteractionRejection.INTERACTION_NOT_FOUND,
            )
        return when (snapshot.state) {
            AutofillInteractionState.Completed,
            AutofillInteractionState.Cancelled,
            AutofillInteractionState.Expired,
            is AutofillInteractionState.Failed,
            -> AutofillAuthorizationOutcome.Rejected(AutofillInteractionRejection.TERMINAL)

            else -> AutofillAuthorizationOutcome.Authorized(snapshot)
        }
    }

    private suspend fun finish(
        id: AutofillInteractionId,
        attempt: Attempt,
        authentication: AuthenticationResult,
    ): AutofillAuthorizationOutcome {
        val outcome = when (authentication) {
            is AuthenticationResult.Success -> authorize(id, attempt.hadFullAccess)
            is AuthenticationResult.Cancelled -> {
                val result = store.finish(id, AutofillInteractionState.Cancelled).update
                val snapshot = (result as? AutofillInteractionUpdate.Updated)?.snapshot
                    ?: return rejectAndComplete(id, attempt, AutofillInteractionRejection.INVALID_STATE)
                AutofillAuthorizationOutcome.Cancelled(snapshot, authentication.reason)
            }

            is AuthenticationResult.Failure -> {
                val result = store.fail(id, authentication.failure).update
                val snapshot = (result as? AutofillInteractionUpdate.Updated)?.snapshot
                    ?: return rejectAndComplete(id, attempt, AutofillInteractionRejection.INVALID_STATE)
                AutofillAuthorizationOutcome.Failed(snapshot, authentication.failure)
            }
        }
        complete(id, attempt, outcome)
        return outcome
    }

    private suspend fun authorize(
        id: AutofillInteractionId,
        hadFullAccess: Boolean,
    ): AutofillAuthorizationOutcome {
        val acquiredUnlock = !hadFullAccess && vaultAccessState.hasFullSecureSessionAccess()
        val update = store.markAuthorized(id)
        val snapshot = (update as? AutofillInteractionUpdate.Updated)?.snapshot
        if (snapshot == null) {
            if (acquiredUnlock) {
                unlockLease.acquireOrBorrow(id, acquiredUnlock = true)
                unlockLease.release(id)
            }
            return AutofillAuthorizationOutcome.Rejected(
                (update as AutofillInteractionUpdate.Rejected).reason,
            )
        }
        unlockLease.acquireOrBorrow(
            interactionId = id,
            acquiredUnlock = acquiredUnlock,
        )
        return AutofillAuthorizationOutcome.Authorized(snapshot)
    }

    private suspend fun cancelAttempt(id: AutofillInteractionId, attempt: Attempt) {
        val result = store.finish(id, AutofillInteractionState.Cancelled).update
        val snapshot = (result as? AutofillInteractionUpdate.Updated)?.snapshot
        val outcome = if (snapshot == null) {
            AutofillAuthorizationOutcome.Rejected(AutofillInteractionRejection.INVALID_STATE)
        } else {
            AutofillAuthorizationOutcome.Cancelled(snapshot, CancellationReason.CALLER)
        }
        complete(id, attempt, outcome)
    }

    private suspend fun rejectAndComplete(
        id: AutofillInteractionId,
        attempt: Attempt,
        rejection: AutofillInteractionRejection,
    ): AutofillAuthorizationOutcome.Rejected {
        val outcome = AutofillAuthorizationOutcome.Rejected(rejection)
        complete(id, attempt, outcome)
        return outcome
    }

    private suspend fun complete(
        id: AutofillInteractionId,
        attempt: Attempt,
        outcome: AutofillAuthorizationOutcome,
    ) {
        mutex.withLock {
            if (attempts[id] === attempt.result) attempts.remove(id)
        }
        attempt.result.complete(outcome)
    }
}

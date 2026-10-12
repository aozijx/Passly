package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owns authorization and vault-lock lifetime for every Autofill entry point. */
@Singleton
class AutofillInteractionCoordinator @Inject constructor(
    private val authenticationRequester: AuthenticationRequester,
    private val sessionLockController: SessionLockController,
    private val vaultAccessState: SecureSessionAccessState,
) {
    private data class InteractionRecord(
        var snapshot: AutofillInteractionSnapshot,
        var authorization: CompletableDeferred<AutofillAuthorizationOutcome>? = null,
    )

    private data class AuthorizationAttempt(
        val deferred: CompletableDeferred<AutofillAuthorizationOutcome>,
        val ownsAttempt: Boolean,
        val hadFullAccess: Boolean,
    )

    private val mutex = Mutex()
    private val interactions = mutableMapOf<AutofillInteractionId, InteractionRecord>()
    private val unlockBorrowers = mutableSetOf<AutofillInteractionId>()
    private var ownsAutofillUnlock = false

    suspend fun create(
        source: AutofillInteractionSource,
        scope: AutofillGrantContext,
        requireAuthentication: Boolean,
    ): AutofillInteractionSnapshot = mutex.withLock {
        val normalizedScope = scope.normalized()
        require(normalizedScope.packageName.isNotBlank()) { "Autofill package name cannot be blank" }

        val hasAccess = vaultAccessState.hasFullSecureSessionAccess()
        val state = if (hasAccess && !requireAuthentication) {
            AutofillInteractionState.Authorized
        } else {
            AutofillInteractionState.RequiresUnlock
        }
        val snapshot = AutofillInteractionSnapshot(
            id = AutofillInteractionId.new(),
            source = source,
            scope = normalizedScope,
            state = state,
        )
        interactions[snapshot.id] = InteractionRecord(snapshot)
        if (state == AutofillInteractionState.Authorized && ownsAutofillUnlock) {
            unlockBorrowers += snapshot.id
        }
        snapshot
    }

    suspend fun authorize(id: AutofillInteractionId): AutofillAuthorizationOutcome {
        val attempt = mutex.withLock {
            val record = interactions[id]
                ?: return AutofillAuthorizationOutcome.Rejected(
                    AutofillInteractionRejection.INTERACTION_NOT_FOUND,
                )
            when (record.snapshot.state) {
                AutofillInteractionState.RequiresUnlock -> {
                    record.authorization?.let {
                        AuthorizationAttempt(it, ownsAttempt = false, hadFullAccess = false)
                    } ?: CompletableDeferred<AutofillAuthorizationOutcome>().let { deferred ->
                        record.authorization = deferred
                        AuthorizationAttempt(
                            deferred = deferred,
                            ownsAttempt = true,
                            hadFullAccess = vaultAccessState.hasFullSecureSessionAccess(),
                        )
                    }
                }

                AutofillInteractionState.Cancelled,
                AutofillInteractionState.Expired,
                AutofillInteractionState.Completed,
                is AutofillInteractionState.Failed,
                -> return AutofillAuthorizationOutcome.Rejected(AutofillInteractionRejection.TERMINAL)

                else -> return AutofillAuthorizationOutcome.Authorized(record.snapshot)
            }
        }

        if (!attempt.ownsAttempt) return attempt.deferred.await()

        return try {
            val result = authenticationRequester.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL),
            )
            finishAuthorization(id, attempt, result)
        } catch (cancelled: CancellationException) {
            cancelAuthorizationAttempt(id, attempt)
            throw cancelled
        }
    }

    suspend fun recordCandidates(
        id: AutofillInteractionId,
        candidateIds: Set<EntryId>,
    ): AutofillInteractionUpdate = update(id) { snapshot ->
        if (snapshot.state != AutofillInteractionState.Authorized &&
            snapshot.state != AutofillInteractionState.CandidatesReady
        ) {
            return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
        }
        AutofillInteractionUpdate.Updated(
            snapshot.copy(
                state = AutofillInteractionState.CandidatesReady,
                presentedCandidateIds = candidateIds.toSet(),
            ),
        )
    }

    suspend fun beginSelection(
        id: AutofillInteractionId,
        entryId: EntryId,
    ): AutofillInteractionUpdate = update(id) { snapshot ->
        if (snapshot.state != AutofillInteractionState.CandidatesReady) {
            return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
        }
        if (entryId !in snapshot.presentedCandidateIds) {
            return@update AutofillInteractionUpdate.Rejected(
                AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED,
            )
        }
        AutofillInteractionUpdate.Updated(
            snapshot.copy(state = AutofillInteractionState.ResolvingSelection(entryId)),
        )
    }

    suspend fun markFilledAwaitingSave(id: AutofillInteractionId): AutofillInteractionUpdate =
        transition(
            id = id,
            allowedState = { it is AutofillInteractionState.ResolvingSelection },
            nextState = AutofillInteractionState.FilledAwaitingSave,
        )

    suspend fun beginSave(id: AutofillInteractionId): AutofillInteractionUpdate =
        transition(
            id = id,
            allowedState = { it == AutofillInteractionState.FilledAwaitingSave },
            nextState = AutofillInteractionState.Saving,
        )

    suspend fun findAwaitingSave(scope: AutofillGrantContext): AutofillInteractionSnapshot? =
        mutex.withLock {
            val normalizedScope = scope.normalized()
            interactions.values
                .asSequence()
                .map { it.snapshot }
                .lastOrNull {
                    it.scope == normalizedScope &&
                        it.state == AutofillInteractionState.FilledAwaitingSave
                }
        }

    suspend fun complete(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Completed)

    suspend fun cancel(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Cancelled)

    suspend fun expire(id: AutofillInteractionId): AutofillInteractionUpdate =
        finish(id, AutofillInteractionState.Expired)

    suspend fun snapshot(id: AutofillInteractionId): AutofillInteractionSnapshot? =
        mutex.withLock { interactions[id]?.snapshot }

    private suspend fun finishAuthorization(
        id: AutofillInteractionId,
        attempt: AuthorizationAttempt,
        result: AuthenticationResult,
    ): AutofillAuthorizationOutcome = mutex.withLock {
        val record = interactions[id]
        val outcome = if (record == null || record.authorization !== attempt.deferred) {
            AutofillAuthorizationOutcome.Rejected(AutofillInteractionRejection.INTERACTION_NOT_FOUND)
        } else {
            when (result) {
                is AuthenticationResult.Success -> {
                    if (!attempt.hadFullAccess && vaultAccessState.hasFullSecureSessionAccess()) {
                        ownsAutofillUnlock = true
                    }
                    if (ownsAutofillUnlock) unlockBorrowers += id
                    val updated = record.snapshot.copy(state = AutofillInteractionState.Authorized)
                    record.snapshot = updated
                    AutofillAuthorizationOutcome.Authorized(updated)
                }

                is AuthenticationResult.Cancelled -> {
                    val updated = record.snapshot.copy(state = AutofillInteractionState.Cancelled)
                    record.snapshot = updated
                    AutofillAuthorizationOutcome.Cancelled(updated, result.reason)
                }

                is AuthenticationResult.Failure -> {
                    val updated = record.snapshot.copy(
                        state = AutofillInteractionState.Failed(result.failure),
                    )
                    record.snapshot = updated
                    AutofillAuthorizationOutcome.Failed(updated, result.failure)
                }
            }
        }
        record?.authorization = null
        attempt.deferred.complete(outcome)
        outcome
    }

    private suspend fun cancelAuthorizationAttempt(
        id: AutofillInteractionId,
        attempt: AuthorizationAttempt,
    ) {
        mutex.withLock {
            val record = interactions[id] ?: return@withLock
            if (record.authorization !== attempt.deferred) return@withLock
            val updated = record.snapshot.copy(state = AutofillInteractionState.Cancelled)
            record.snapshot = updated
            record.authorization = null
            attempt.deferred.complete(
                AutofillAuthorizationOutcome.Cancelled(
                    snapshot = updated,
                    reason = com.aozijx.passly.domain.access.model.CancellationReason.CALLER,
                ),
            )
        }
    }

    private suspend fun transition(
        id: AutofillInteractionId,
        allowedState: (AutofillInteractionState) -> Boolean,
        nextState: AutofillInteractionState,
    ): AutofillInteractionUpdate = update(id) { snapshot ->
        if (!allowedState(snapshot.state)) {
            AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
        } else {
            AutofillInteractionUpdate.Updated(snapshot.copy(state = nextState))
        }
    }

    private suspend fun update(
        id: AutofillInteractionId,
        transform: (AutofillInteractionSnapshot) -> AutofillInteractionUpdate,
    ): AutofillInteractionUpdate = mutex.withLock {
        val record = interactions[id]
            ?: return AutofillInteractionUpdate.Rejected(
                AutofillInteractionRejection.INTERACTION_NOT_FOUND,
            )
        val update = transform(record.snapshot)
        if (update is AutofillInteractionUpdate.Updated) record.snapshot = update.snapshot
        update
    }

    private suspend fun finish(
        id: AutofillInteractionId,
        terminalState: AutofillInteractionState,
    ): AutofillInteractionUpdate = mutex.withLock {
        val record = interactions[id]
            ?: return AutofillInteractionUpdate.Rejected(
                AutofillInteractionRejection.INTERACTION_NOT_FOUND,
            )
        if (record.snapshot.state.isTerminal()) {
            return AutofillInteractionUpdate.Updated(record.snapshot)
        }

        val updated = record.snapshot.copy(state = terminalState)
        record.snapshot = updated
        record.authorization?.complete(
            AutofillAuthorizationOutcome.Rejected(AutofillInteractionRejection.TERMINAL),
        )
        record.authorization = null
        unlockBorrowers -= id
        if (ownsAutofillUnlock && unlockBorrowers.isEmpty()) {
            ownsAutofillUnlock = false
            sessionLockController.lock(LockReason.AUTOFILL_REQUEST_FINISHED)
        }
        AutofillInteractionUpdate.Updated(updated)
    }

    private fun AutofillInteractionState.isTerminal(): Boolean = when (this) {
        AutofillInteractionState.Completed,
        AutofillInteractionState.Cancelled,
        AutofillInteractionState.Expired,
        is AutofillInteractionState.Failed,
        -> true

        else -> false
    }
}

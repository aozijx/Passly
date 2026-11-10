package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillWebConsent
import com.aozijx.passly.domain.entry.model.EntryId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class AutofillFinishResult(
    val update: AutofillInteractionUpdate,
    val transitioned: Boolean,
)

/** Process-local interaction state. It owns no authentication or vault policy. */
@Singleton
internal class AutofillInteractionStore
    @Inject
    constructor() {
        private val mutex = Mutex()
        private val interactions = linkedMapOf<AutofillInteractionId, AutofillInteractionSnapshot>()

        suspend fun create(
            source: AutofillInteractionSource,
            scope: AutofillGrantContext,
            initialState: AutofillInteractionState,
        ): AutofillInteractionSnapshot =
            mutex.withLock {
                val normalizedScope = requireNotNull(scope.normalized()) { "Invalid Autofill interaction scope" }
                require(
                    initialState == AutofillInteractionState.RequiresUnlock ||
                        initialState == AutofillInteractionState.Authorized,
                ) { "New Autofill interaction must require unlock or be authorized" }

                AutofillInteractionSnapshot(
                    id = AutofillInteractionId.new(),
                    source = source,
                    scope = normalizedScope,
                    state = initialState,
                ).also { interactions[it.id] = it }
            }

        suspend fun snapshot(id: AutofillInteractionId): AutofillInteractionSnapshot? = mutex.withLock { interactions[id] }

        suspend fun beginAuthorization(id: AutofillInteractionId): AutofillInteractionUpdate =
            transition(
                id = id,
                allowed = { it == AutofillInteractionState.RequiresUnlock },
                next = AutofillInteractionState.Authenticating,
            )

        suspend fun authorizeFromExistingSession(id: AutofillInteractionId): AutofillInteractionUpdate =
            transition(
                id = id,
                allowed = { it == AutofillInteractionState.RequiresUnlock },
                next = AutofillInteractionState.Authorized,
            )

        suspend fun markAuthorized(id: AutofillInteractionId): AutofillInteractionUpdate =
            transition(
                id = id,
                allowed = { it == AutofillInteractionState.Authenticating },
                next = AutofillInteractionState.Authorized,
            )

        suspend fun confirmWebRecipient(
            id: AutofillInteractionId,
            scope: AutofillGrantContext,
            sessionAtMs: Long,
        ): AutofillInteractionUpdate =
            update(id) { snapshot ->
                if (snapshot.scope != scope.normalized()) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.SCOPE_MISMATCH)
                }
                if (snapshot.source != AutofillInteractionSource.LEGACY || snapshot.scope.webDomain == null) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
                if (snapshot.state != AutofillInteractionState.Authorized) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
                AutofillInteractionUpdate.Updated(
                    snapshot.copy(
                        webConsent = AutofillWebConsent.RecipientConfirmed(sessionAtMs),
                    ),
                )
            }

        suspend fun confirmWebEntry(
            id: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            sessionAtMs: Long,
        ): AutofillInteractionUpdate =
            update(id) { snapshot ->
                if (snapshot.scope != scope.normalized()) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.SCOPE_MISMATCH)
                }
                if (snapshot.source != AutofillInteractionSource.LEGACY || snapshot.scope.webDomain == null) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
                val consent = snapshot.webConsent as? AutofillWebConsent.Confirmed
                if (consent == null) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.WEB_ORIGIN_NOT_CONFIRMED,
                    )
                }
                if (consent.sessionAtMs != sessionAtMs) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.WEB_CONFIRMATION_EXPIRED,
                    )
                }
                if (snapshot.state != AutofillInteractionState.CandidatesReady) {
                    return@update AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
                if (entryId !in snapshot.presentedCandidateIds) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED,
                    )
                }
                AutofillInteractionUpdate.Updated(
                    snapshot.copy(webConsent = AutofillWebConsent.EntryConfirmed(sessionAtMs, entryId)),
                )
            }

        suspend fun isConfirmedWebSelectionReady(
            id: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            sessionAtMs: Long,
        ): Boolean =
            mutex.withLock {
                val snapshot = interactions[id] ?: return@withLock false
                snapshot.scope == scope.normalized() &&
                    snapshot.source == AutofillInteractionSource.LEGACY &&
                    snapshot.scope.webDomain != null &&
                    (snapshot.webConsent as? AutofillWebConsent.EntryConfirmed)?.let {
                        it.sessionAtMs == sessionAtMs && it.entryId == entryId
                    } == true &&
                    snapshot.state == AutofillInteractionState.ResolvingSelection(entryId)
            }

        suspend fun claimWebResultDelivery(
            id: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            sessionAtMs: Long,
        ): Boolean =
            mutex.withLock {
                val snapshot = interactions[id] ?: return@withLock false
                if (snapshot.source != AutofillInteractionSource.LEGACY || snapshot.scope.webDomain == null) {
                    return@withLock false
                }
                val consent = snapshot.webConsent as? AutofillWebConsent.EntryConfirmed
                if (snapshot.scope != scope.normalized() ||
                    consent == null || consent.sessionAtMs != sessionAtMs || consent.entryId != entryId ||
                    snapshot.state != AutofillInteractionState.ResolvingSelection(entryId) ||
                    consent.deliveryClaimed
                ) {
                    return@withLock false
                }
                interactions[id] = snapshot.copy(webConsent = consent.copy(deliveryClaimed = true))
                true
            }

        suspend fun releaseWebResultDeliveryClaim(
            id: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            sessionAtMs: Long,
        ) {
            mutex.withLock {
                val snapshot = interactions[id] ?: return@withLock
                val consent = snapshot.webConsent as? AutofillWebConsent.EntryConfirmed
                if (snapshot.scope == scope.normalized() &&
                    snapshot.state == AutofillInteractionState.ResolvingSelection(entryId) &&
                    consent != null && consent.sessionAtMs == sessionAtMs && consent.entryId == entryId &&
                    consent.deliveryClaimed
                ) {
                    interactions[id] = snapshot.copy(webConsent = consent.copy(deliveryClaimed = false))
                }
            }
        }

        suspend fun recordCandidates(
            id: AutofillInteractionId,
            candidateIds: Set<EntryId>,
            sessionAtMs: Long? = null,
        ): AutofillInteractionUpdate =
            update(id) { snapshot ->
                val consent = snapshot.webConsent
                if (snapshot.state != AutofillInteractionState.Authorized &&
                    snapshot.state != AutofillInteractionState.CandidatesReady
                ) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.INVALID_STATE,
                    )
                }
                if (snapshot.source == AutofillInteractionSource.LEGACY &&
                    snapshot.scope.webDomain != null &&
                    (consent !is AutofillWebConsent.Confirmed || consent.sessionAtMs != sessionAtMs)
                ) {
                    return@update AutofillInteractionUpdate.Rejected(
                        if (consent is AutofillWebConsent.Confirmed) {
                            AutofillInteractionRejection.WEB_CONFIRMATION_EXPIRED
                        } else {
                            AutofillInteractionRejection.WEB_ORIGIN_NOT_CONFIRMED
                        },
                    )
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
            sessionAtMs: Long? = null,
        ): AutofillInteractionUpdate =
            update(id) { snapshot ->
                val consent = snapshot.webConsent
                if (snapshot.state != AutofillInteractionState.CandidatesReady) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.INVALID_STATE,
                    )
                }
                if (entryId !in snapshot.presentedCandidateIds) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED,
                    )
                }
                if (snapshot.source == AutofillInteractionSource.LEGACY &&
                    snapshot.scope.webDomain != null &&
                    (consent !is AutofillWebConsent.Confirmed || consent.sessionAtMs != sessionAtMs)
                ) {
                    return@update AutofillInteractionUpdate.Rejected(
                        if (consent is AutofillWebConsent.Confirmed) {
                            AutofillInteractionRejection.WEB_CONFIRMATION_EXPIRED
                        } else {
                            AutofillInteractionRejection.WEB_ORIGIN_NOT_CONFIRMED
                        },
                    )
                }
                if (snapshot.source == AutofillInteractionSource.LEGACY &&
                    snapshot.scope.webDomain != null &&
                    (snapshot.webConsent as? AutofillWebConsent.EntryConfirmed)?.entryId != entryId
                ) {
                    return@update AutofillInteractionUpdate.Rejected(
                        AutofillInteractionRejection.WEB_ENTRY_NOT_CONFIRMED,
                    )
                }
                AutofillInteractionUpdate.Updated(
                    snapshot.copy(state = AutofillInteractionState.ResolvingSelection(entryId)),
                )
            }

        suspend fun activeIds(source: AutofillInteractionSource): List<AutofillInteractionId> =
            mutex.withLock {
                interactions.values
                    .filter { it.source == source && !it.state.isTerminal() }
                    .map(AutofillInteractionSnapshot::id)
            }

        suspend fun activeIds(): List<AutofillInteractionId> =
            mutex.withLock {
                interactions.values
                    .filterNot { it.state.isTerminal() }
                    .map(AutofillInteractionSnapshot::id)
            }

        suspend fun removeTerminal(id: AutofillInteractionId) {
            mutex.withLock {
                if (interactions[id]?.state?.isTerminal() == true) {
                    interactions.remove(id)
                }
            }
        }

        suspend fun finish(
            id: AutofillInteractionId,
            terminalState: AutofillInteractionState,
        ): AutofillFinishResult =
            mutex.withLock {
                require(terminalState.isTerminal()) { "Autofill finish state must be terminal" }
                finishLocked(id, terminalState)
            }

        /**
         * Expires an abandoned interaction unless authentication or secret resolution is still active.
         * A null result asks the timeout owner to defer cleanup and try again later.
         */
        suspend fun expireIfIdle(id: AutofillInteractionId): AutofillFinishResult? =
            mutex.withLock {
                val snapshot = interactions[id]
                if ((snapshot?.webConsent as? AutofillWebConsent.EntryConfirmed)?.deliveryClaimed == true) {
                    return@withLock null
                }
                when (snapshot?.state) {
                    AutofillInteractionState.Authenticating,
                    is AutofillInteractionState.ResolvingSelection,
                    -> null

                    else -> finishLocked(id, AutofillInteractionState.Expired)
                }
            }

        private fun finishLocked(
            id: AutofillInteractionId,
            terminalState: AutofillInteractionState,
        ): AutofillFinishResult {
            val snapshot =
                interactions[id]
                    ?: return AutofillFinishResult(
                        AutofillInteractionUpdate.Rejected(
                            AutofillInteractionRejection.INTERACTION_NOT_FOUND,
                        ),
                        transitioned = false,
                    )
            if (snapshot.state.isTerminal()) {
                return AutofillFinishResult(
                    AutofillInteractionUpdate.Updated(snapshot),
                    transitioned = false,
                )
            }
            if ((snapshot.webConsent as? AutofillWebConsent.EntryConfirmed)?.deliveryClaimed == true &&
                terminalState != AutofillInteractionState.Completed
            ) {
                return AutofillFinishResult(
                    AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE),
                    transitioned = false,
                )
            }

            val updated = snapshot.copy(state = terminalState)
            interactions[id] = updated
            return AutofillFinishResult(
                AutofillInteractionUpdate.Updated(updated),
                transitioned = true,
            )
        }

        suspend fun fail(
            id: AutofillInteractionId,
            failure: AuthenticationFailure,
        ): AutofillFinishResult = finish(id, AutofillInteractionState.Failed(failure))

        private suspend fun transition(
            id: AutofillInteractionId,
            allowed: (AutofillInteractionState) -> Boolean,
            next: AutofillInteractionState,
        ): AutofillInteractionUpdate =
            update(id) { snapshot ->
                if (allowed(snapshot.state)) {
                    AutofillInteractionUpdate.Updated(snapshot.copy(state = next))
                } else {
                    AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
            }

        private suspend fun update(
            id: AutofillInteractionId,
            transform: (AutofillInteractionSnapshot) -> AutofillInteractionUpdate,
        ): AutofillInteractionUpdate =
            mutex.withLock {
                val snapshot =
                    interactions[id]
                        ?: return AutofillInteractionUpdate.Rejected(
                            AutofillInteractionRejection.INTERACTION_NOT_FOUND,
                        )
                transform(snapshot).also { update ->
                    if (update is AutofillInteractionUpdate.Updated) interactions[id] = update.snapshot
                }
            }

        private fun AutofillInteractionState.isTerminal(): Boolean =
            when (this) {
                AutofillInteractionState.Completed,
                AutofillInteractionState.Cancelled,
                AutofillInteractionState.Expired,
                is AutofillInteractionState.Failed,
                -> true

                else -> false
            }
    }

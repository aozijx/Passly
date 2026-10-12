package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.AuthenticationFailure
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AutofillFinishResult(
    val update: AutofillInteractionUpdate,
    val transitioned: Boolean,
)

/** Process-local interaction state. It owns no authentication or vault policy. */
@Singleton
class AutofillInteractionStore @Inject constructor() {
    private val mutex = Mutex()
    private val interactions = linkedMapOf<AutofillInteractionId, AutofillInteractionSnapshot>()

    suspend fun create(
        source: AutofillInteractionSource,
        scope: AutofillGrantContext,
        initialState: AutofillInteractionState,
    ): AutofillInteractionSnapshot = mutex.withLock {
        val normalizedScope = scope.normalized()
        require(normalizedScope.packageName.isNotBlank()) { "Autofill package name cannot be blank" }
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

    suspend fun snapshot(id: AutofillInteractionId): AutofillInteractionSnapshot? =
        mutex.withLock { interactions[id] }

    suspend fun markAuthorized(id: AutofillInteractionId): AutofillInteractionUpdate =
        transition(
            id = id,
            allowed = { it == AutofillInteractionState.RequiresUnlock },
            next = AutofillInteractionState.Authorized,
        )

    suspend fun recordCandidates(
        id: AutofillInteractionId,
        candidateIds: Set<EntryId>,
        savePromptsEnabled: Boolean = false,
    ): AutofillInteractionUpdate = update(id) { snapshot ->
        if (snapshot.state != AutofillInteractionState.Authorized &&
            snapshot.state != AutofillInteractionState.CandidatesReady
        ) {
            return@update AutofillInteractionUpdate.Rejected(
                AutofillInteractionRejection.INVALID_STATE,
            )
        }
        AutofillInteractionUpdate.Updated(
            snapshot.copy(
                state = AutofillInteractionState.CandidatesReady,
                presentedCandidateIds = candidateIds.toSet(),
                savePromptsEnabled = savePromptsEnabled,
            ),
        )
    }

    suspend fun beginSelection(
        id: AutofillInteractionId,
        entryId: EntryId,
    ): AutofillInteractionUpdate = update(id) { snapshot ->
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
        AutofillInteractionUpdate.Updated(
            snapshot.copy(state = AutofillInteractionState.ResolvingSelection(entryId)),
        )
    }

    suspend fun markFilledAwaitingSave(id: AutofillInteractionId): AutofillInteractionUpdate =
        transition(
            id = id,
            allowed = { it is AutofillInteractionState.ResolvingSelection },
            next = AutofillInteractionState.FilledAwaitingSave,
        )

    suspend fun beginSave(id: AutofillInteractionId): AutofillInteractionUpdate =
        update(id) { snapshot ->
            if (!snapshot.canSave()) {
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
            } else {
                AutofillInteractionUpdate.Updated(
                    snapshot.copy(state = AutofillInteractionState.Saving),
                )
            }
        }

    suspend fun findSaveable(
        scope: AutofillGrantContext,
        source: AutofillInteractionSource,
    ): AutofillInteractionSnapshot? =
        mutex.withLock {
            val normalizedScope = scope.normalized()
            interactions.values.lastOrNull {
                it.scope == normalizedScope &&
                    it.source == source &&
                    it.canSave()
            }
        }

    suspend fun activeIds(source: AutofillInteractionSource): List<AutofillInteractionId> =
        mutex.withLock {
            interactions.values
                .filter { it.source == source && !it.state.isTerminal() }
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
    ): AutofillFinishResult = mutex.withLock {
        require(terminalState.isTerminal()) { "Autofill finish state must be terminal" }
        finishLocked(id, terminalState)
    }

    /**
     * Expires an abandoned interaction unless secret resolution or saving is still active.
     * A null result asks the timeout owner to defer cleanup and try again later.
     */
    suspend fun expireIfIdle(id: AutofillInteractionId): AutofillFinishResult? = mutex.withLock {
        when (interactions[id]?.state) {
            is AutofillInteractionState.ResolvingSelection,
            AutofillInteractionState.Saving,
            -> null

            else -> finishLocked(id, AutofillInteractionState.Expired)
        }
    }

    private fun finishLocked(
        id: AutofillInteractionId,
        terminalState: AutofillInteractionState,
    ): AutofillFinishResult {
        val snapshot = interactions[id]
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
    ): AutofillInteractionUpdate = update(id) { snapshot ->
        if (allowed(snapshot.state)) {
            AutofillInteractionUpdate.Updated(snapshot.copy(state = next))
        } else {
            AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE)
        }
    }

    private suspend fun update(
        id: AutofillInteractionId,
        transform: (AutofillInteractionSnapshot) -> AutofillInteractionUpdate,
    ): AutofillInteractionUpdate = mutex.withLock {
        val snapshot = interactions[id]
            ?: return AutofillInteractionUpdate.Rejected(
                AutofillInteractionRejection.INTERACTION_NOT_FOUND,
            )
        transform(snapshot).also { update ->
            if (update is AutofillInteractionUpdate.Updated) interactions[id] = update.snapshot
        }
    }

    private fun AutofillInteractionState.isTerminal(): Boolean = when (this) {
        AutofillInteractionState.Completed,
        AutofillInteractionState.Cancelled,
        AutofillInteractionState.Expired,
        is AutofillInteractionState.Failed,
        -> true

        else -> false
    }

    private fun AutofillInteractionSnapshot.canSave(): Boolean =
        state == AutofillInteractionState.FilledAwaitingSave ||
            (state == AutofillInteractionState.CandidatesReady && savePromptsEnabled)
}

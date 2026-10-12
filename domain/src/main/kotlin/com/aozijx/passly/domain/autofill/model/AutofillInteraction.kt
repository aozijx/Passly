package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.entry.model.EntryId
import com.github.f4b6a3.uuid.UuidCreator

@JvmInline
value class AutofillInteractionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Autofill interaction ID cannot be blank" }
    }

    companion object {
        fun new(): AutofillInteractionId =
            AutofillInteractionId(UuidCreator.getTimeOrderedEpoch().toString())
    }
}

enum class AutofillInteractionSource {
    LEGACY,
    CREDENTIAL_MANAGER,
}

sealed interface AutofillInteractionState {
    data object Created : AutofillInteractionState
    data object RequiresUnlock : AutofillInteractionState
    data object Authorized : AutofillInteractionState
    data object CandidatesReady : AutofillInteractionState
    data class ResolvingSelection(val entryId: EntryId) : AutofillInteractionState
    data object FilledAwaitingSave : AutofillInteractionState
    data object Saving : AutofillInteractionState
    data object Completed : AutofillInteractionState
    data object Cancelled : AutofillInteractionState
    data object Expired : AutofillInteractionState
    data class Failed(val failure: AuthenticationFailure) : AutofillInteractionState
}

data class AutofillInteractionSnapshot(
    val id: AutofillInteractionId,
    val source: AutofillInteractionSource,
    val scope: AutofillGrantContext,
    val state: AutofillInteractionState,
    val presentedCandidateIds: Set<EntryId> = emptySet(),
)

enum class AutofillInteractionRejection {
    INTERACTION_NOT_FOUND,
    INVALID_STATE,
    CANDIDATE_NOT_PRESENTED,
    SCOPE_MISMATCH,
    TERMINAL,
}

sealed interface AutofillInteractionUpdate {
    data class Updated(val snapshot: AutofillInteractionSnapshot) : AutofillInteractionUpdate
    data class Rejected(val reason: AutofillInteractionRejection) : AutofillInteractionUpdate
}

sealed interface AutofillAuthorizationOutcome {
    data class Authorized(val snapshot: AutofillInteractionSnapshot) : AutofillAuthorizationOutcome
    data class Cancelled(
        val snapshot: AutofillInteractionSnapshot,
        val reason: CancellationReason,
    ) : AutofillAuthorizationOutcome

    data class Failed(
        val snapshot: AutofillInteractionSnapshot,
        val failure: AuthenticationFailure,
    ) : AutofillAuthorizationOutcome

    data class Rejected(val reason: AutofillInteractionRejection) : AutofillAuthorizationOutcome
}

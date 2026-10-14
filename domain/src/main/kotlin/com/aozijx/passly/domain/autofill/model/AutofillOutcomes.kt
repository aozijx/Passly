package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation

data class AutofillDiscoveryContext(
    val roleMap: Map<String, FieldRole>,
    val presentation: AutofillPresentation,
    val requireAuthentication: Boolean,
    val includeOtp: Boolean,
    val savePromptsEnabled: Boolean,
)

enum class AutofillOperationFailure {
    INTERACTION_NOT_FOUND,
    INVALID_INTERACTION_STATE,
    SCOPE_MISMATCH,
    CANDIDATE_QUERY_FAILED,
    CREDENTIAL_RESOLUTION_FAILED,
    PERSISTENCE_FAILED,
}

sealed interface AutofillDiscoveryOutcome {
    data object Disabled : AutofillDiscoveryOutcome
    data object UnsupportedFields : AutofillDiscoveryOutcome

    data class RequiresUnlock(
        val snapshot: AutofillInteractionSnapshot,
        val context: AutofillDiscoveryContext,
    ) : AutofillDiscoveryOutcome

    data class NoCandidates(
        val snapshot: AutofillInteractionSnapshot,
        val context: AutofillDiscoveryContext,
    ) : AutofillDiscoveryOutcome

    data class Candidates(
        val snapshot: AutofillInteractionSnapshot,
        val candidates: List<AutofillCandidateSummary>,
        val context: AutofillDiscoveryContext,
    ) : AutofillDiscoveryOutcome

    data class Failed(val failure: AutofillOperationFailure) : AutofillDiscoveryOutcome
}

sealed interface AutofillSelectionOutcome {
    data class Filled(val payload: AutofillCredentialPayload) : AutofillSelectionOutcome
    data object Cancelled : AutofillSelectionOutcome
    data object Unauthorized : AutofillSelectionOutcome
    data object CandidateUnavailable : AutofillSelectionOutcome
    data object ScopeMismatch : AutofillSelectionOutcome
    data class Rejected(val reason: AutofillInteractionRejection) : AutofillSelectionOutcome
    data class Failed(val failure: AutofillOperationFailure) : AutofillSelectionOutcome
}

sealed interface AutofillSaveOutcome {
    data class Saved(val entryId: EntryId) : AutofillSaveOutcome
    data object InvalidDraft : AutofillSaveOutcome
    data class Failed(val failure: AutofillOperationFailure) : AutofillSaveOutcome
}

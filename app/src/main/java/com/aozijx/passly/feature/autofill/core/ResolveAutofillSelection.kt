package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ResolveAutofillSelection @Inject constructor(
    private val interactionStore: AutofillInteractionStore,
    private val credentialResolver: AutofillCredentialResolver,
) {
    suspend operator fun invoke(
        interactionId: AutofillInteractionId,
        scope: AutofillGrantContext,
        entryId: EntryId,
        includeOtp: Boolean,
    ): AutofillSelectionOutcome {
        val snapshot = interactionStore.snapshot(interactionId)
            ?: return AutofillSelectionOutcome.Rejected(
                AutofillInteractionRejection.INTERACTION_NOT_FOUND,
            )
        if (snapshot.scope != scope.normalized()) return AutofillSelectionOutcome.ScopeMismatch
        when (snapshot.state) {
            AutofillInteractionState.RequiresUnlock,
            AutofillInteractionState.Authenticating,
            -> return AutofillSelectionOutcome.Unauthorized
            AutofillInteractionState.Cancelled -> return AutofillSelectionOutcome.Cancelled
            else -> Unit
        }

        return when (val selection = interactionStore.beginSelection(interactionId, entryId)) {
            is AutofillInteractionUpdate.Rejected -> AutofillSelectionOutcome.Rejected(selection.reason)
            is AutofillInteractionUpdate.Updated -> resolve(entryId, includeOtp)
        }
    }

    private suspend fun resolve(
        entryId: EntryId,
        includeOtp: Boolean,
    ): AutofillSelectionOutcome = try {
        when (
            val resolution = credentialResolver.resolve(
                AutofillCredentialSelection(entryId, includeOtp),
            )
        ) {
            is AutofillCredentialResolution.Resolved -> {
                AutofillSelectionOutcome.Filled(resolution.payload)
            }

            AutofillCredentialResolution.NotFound,
            AutofillCredentialResolution.NotFillable,
            -> AutofillSelectionOutcome.CandidateUnavailable
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        AutofillSelectionOutcome.Failed(AutofillOperationFailure.CREDENTIAL_RESOLUTION_FAILED)
    }
}

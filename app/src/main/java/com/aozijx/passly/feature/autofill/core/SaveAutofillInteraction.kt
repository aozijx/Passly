package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSaveSource
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

sealed interface AutofillSaveInteractionOutcome {
    data class Saved(val entryId: EntryId) : AutofillSaveInteractionOutcome
    data object InteractionUnavailable : AutofillSaveInteractionOutcome
    data class InteractionRejected(
        val reason: AutofillInteractionRejection,
    ) : AutofillSaveInteractionOutcome

    data class DraftRejected(
        val reason: AutofillSaveDraftRejection,
    ) : AutofillSaveInteractionOutcome

    data object Disabled : AutofillSaveInteractionOutcome
    data class Failed(val failure: AutofillOperationFailure) : AutofillSaveInteractionOutcome
}

class SaveAutofillInteraction @Inject constructor(
    private val interactionStore: AutofillInteractionStore,
    private val draftExtractor: AutofillSaveDraftExtractor,
    private val saveCredential: SaveAutofillCredential,
    private val finishInteraction: FinishAutofillInteraction,
) {
    suspend operator fun invoke(
        request: AutofillRequest,
        interactionSource: AutofillInteractionSource,
        saveSource: AutofillSaveSource,
    ): AutofillSaveInteractionOutcome {
        val scope = AutofillGrantContext(request.packageName, request.domain)
        val interaction = interactionStore.findSaveable(scope, interactionSource)
            ?: return AutofillSaveInteractionOutcome.InteractionUnavailable
        val begun = interactionStore.beginSave(interaction.id)
        if (begun is AutofillInteractionUpdate.Rejected) {
            return AutofillSaveInteractionOutcome.InteractionRejected(begun.reason)
        }

        return try {
            when (val extraction = draftExtractor(request)) {
                is AutofillSaveDraftExtraction.Rejected -> {
                    AutofillSaveInteractionOutcome.DraftRejected(extraction.reason)
                }

                is AutofillSaveDraftExtraction.Ready -> when (
                    val outcome = saveCredential(extraction.draft, saveSource)
                ) {
                    is AutofillSaveOutcome.Saved -> {
                        AutofillSaveInteractionOutcome.Saved(outcome.entryId)
                    }

                    AutofillSaveOutcome.Disabled -> AutofillSaveInteractionOutcome.Disabled
                    AutofillSaveOutcome.InvalidDraft -> {
                        AutofillSaveInteractionOutcome.DraftRejected(
                            AutofillSaveDraftRejection.PASSWORD_MISSING,
                        )
                    }

                    is AutofillSaveOutcome.Failed -> {
                        AutofillSaveInteractionOutcome.Failed(outcome.failure)
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                finishInteraction.complete(interaction.id)
            }
        }
    }
}

package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Closes a delivered selection once and records its usage exactly once. */
class CompleteAutofillSelection @Inject constructor(
    private val recordUsage: RecordAutofillUsageUseCase,
    private val finishInteraction: FinishAutofillInteraction,
) {
    suspend operator fun invoke(
        interactionId: AutofillInteractionId,
        entryId: EntryId,
    ) {
        withContext(NonCancellable) {
            finishInteraction.complete(interactionId) {
                recordUsage(entryId.value)
            }
        }
    }
}

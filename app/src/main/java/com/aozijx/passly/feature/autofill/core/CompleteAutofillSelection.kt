package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Closes a delivered selection once and records its usage exactly once. */
class CompleteAutofillSelection @Inject constructor(
    private val activityRecorder: ActivityRecorder,
    private val finishInteraction: FinishAutofillInteraction,
) {
    suspend operator fun invoke(
        interactionId: AutofillInteractionId,
        entryId: EntryId,
    ) {
        withContext(NonCancellable) {
            finishInteraction.complete(interactionId) {
                activityRecorder.recordUsage(entryId.value, ActivityType.AUTOFILL)
            }
        }
    }

    /** Invalidates picker/auth interactions that never committed a result to Android. */
    suspend fun expireAbandonedInteractions() = withContext(NonCancellable) {
        finishInteraction.expireAll()
    }
}

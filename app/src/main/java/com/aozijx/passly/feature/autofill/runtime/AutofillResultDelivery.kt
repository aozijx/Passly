package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.core.CompleteAutofillSelection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class AutofillSelectionReceipt(
    val interactionId: AutofillInteractionId,
    val entryId: EntryId,
)

/** Closes a temporary Autofill unlock only after Android has received its result. */
@Singleton
class AutofillResultDelivery internal constructor(
    private val completeSelection: CompleteAutofillSelection,
    private val unlockLease: AutofillUnlockLease,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        completeSelection: CompleteAutofillSelection,
        unlockLease: AutofillUnlockLease,
    ) : this(
        completeSelection = completeSelection,
        unlockLease = unlockLease,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    /** Publishes the already-built platform result before releasing Autofill-owned access. */
    fun deliverResult(
        receipt: AutofillSelectionReceipt,
        deliverToPlatform: () -> Unit,
    ) {
        deliverToPlatform()
        unlockLease.stopAcceptingNewBorrowers(receipt.interactionId)
        delivered(receipt)
    }

    private fun delivered(receipt: AutofillSelectionReceipt) {
        scope.launch {
            try {
                completeSelection(receipt.interactionId, receipt.entryId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                TelemetryRuntime.e(
                    EventCategory.AUTOFILL,
                    "autofill.result_delivery_failed",
                    throwable = error,
                )
            }
        }
    }

}

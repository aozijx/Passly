package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
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
import kotlinx.coroutines.runBlocking

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
    private val logger: Logger,
    private val continuations: TraceContinuationStore,
) {
    @Inject
    constructor(
        completeSelection: CompleteAutofillSelection,
        unlockLease: AutofillUnlockLease,
        logger: Logger,
        continuations: TraceContinuationStore,
    ) : this(
        completeSelection = completeSelection,
        unlockLease = unlockLease,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        logger = logger,
        continuations = continuations,
    )

    /** Publishes the already-built platform result before releasing Autofill-owned access. */
    fun deliverResult(
        receipt: AutofillSelectionReceipt,
        deliverToPlatform: () -> Unit,
    ) {
        runBlocking {
            continuations.continueOrRoot(
                receipt.interactionId.value,
                Operations.AutofillResultDelivery,
            ) {
                logger.log(LogStatements.Autofill.resultDeliveryStarted())
                try {
                    deliverToPlatform()
                    logger.log(LogStatements.Autofill.resultDelivered())
                    continuations.capture(receipt.interactionId.value)
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.resultDeliveryFailed(error))
                    throw error
                }
            }
        }
        unlockLease.stopAcceptingNewBorrowers(receipt.interactionId)
        delivered(receipt)
    }

    private fun delivered(receipt: AutofillSelectionReceipt) {
        scope.launch {
            try {
                continuations.continueOrRoot(
                    receipt.interactionId.value,
                    Operations.AutofillSessionClose,
                ) {
                    logger.log(LogStatements.Autofill.sessionCloseStarted())
                    try {
                        completeSelection(receipt.interactionId, receipt.entryId)
                        logger.log(LogStatements.Autofill.sessionClosed())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        logger.log(LogStatements.Autofill.sessionCloseFailed(error))
                        throw error
                    } finally {
                        continuations.discard(receipt.interactionId.value)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Failure was recorded inside the close span; platform delivery already succeeded.
            }
        }
    }
}

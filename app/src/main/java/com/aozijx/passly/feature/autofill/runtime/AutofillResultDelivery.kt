package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.monotonicTimeMs
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

    /** Commits the result to Android before releasing Autofill-owned access. */
    fun commitResult(
        receipt: AutofillSelectionReceipt,
        commitToPlatform: () -> Unit,
    ) {
        unlockLease.beginResultCompletion(receipt.interactionId)
        var committed = false
        var platformCommitted = false
        try {
            try {
                runBlocking {
                    continuations.continueOrRoot(
                        receipt.interactionId.value,
                        Operations.AutofillResultDelivery,
                    ) {
                        val deliveryStartedAt = monotonicTimeMs()
                        logger.log(LogStatements.Autofill.resultDeliveryStarted())
                        try {
                            commitToPlatform()
                            platformCommitted = true
                        } catch (error: Exception) {
                            try {
                                logger.log(
                                    LogStatements.Autofill.resultDeliveryFailed(
                                        error,
                                        monotonicTimeMs() - deliveryStartedAt,
                                    ),
                                )
                            } catch (_: Exception) {
                                // Preserve the platform callback failure as the primary result.
                            }
                            throw error
                        }
                        // Once the platform callback returns, observability failure must not
                        // turn a delivered credential into a cancelled Activity result.
                        try {
                            logger.log(
                                LogStatements.Autofill.resultDelivered(
                                    monotonicTimeMs() - deliveryStartedAt,
                                ),
                            )
                            continuations.capture(receipt.interactionId.value)
                        } catch (_: Exception) {
                            // The platform has the result; selection completion must continue.
                        }
                    }
                }
            } catch (error: Exception) {
                if (!platformCommitted) throw error
            }
            committed = true
        } finally {
            if (!committed) {
                runBlocking { unlockLease.endResultCompletion {} }
            }
        }
        completeDeliveredSelection(receipt)
    }

    private fun completeDeliveredSelection(receipt: AutofillSelectionReceipt) {
        scope.launch {
            var selectionCompleted = false
            val closeStartedAt = monotonicTimeMs()
            try {
                try {
                    continuations.continueOrRoot(
                        receipt.interactionId.value,
                        Operations.AutofillSessionClose,
                    ) {
                        logger.log(LogStatements.Autofill.sessionCloseStarted())
                        completeSelection(receipt.interactionId, receipt.entryId)
                        selectionCompleted = true
                        logger.log(
                            LogStatements.Autofill.sessionClosed(
                                monotonicTimeMs() - closeStartedAt,
                            ),
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Trace/logging may fail before completion runs. Android already owns the
                    // result, so finish the interaction without a trace too.
                    if (!selectionCompleted) {
                        completeSelection(receipt.interactionId, receipt.entryId)
                        selectionCompleted = true
                    }
                    try {
                        logger.log(
                            LogStatements.Autofill.sessionCloseFailed(
                                error,
                                monotonicTimeMs() - closeStartedAt,
                            ),
                        )
                    } catch (_: Exception) {
                        // Diagnostics must not block lease release after platform delivery.
                    }
                }
            } finally {
                try {
                    continuations.discard(receipt.interactionId.value)
                } finally {
                    unlockLease.endResultCompletion {
                        completeSelection.expireAbandonedInteractions()
                    }
                }
            }
        }
    }
}

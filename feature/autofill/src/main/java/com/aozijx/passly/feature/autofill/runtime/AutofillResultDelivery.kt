package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillWebConsent
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.core.CompleteAutofillSelection
import com.aozijx.passly.feature.autofill.core.autofillSessionGeneration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal data class AutofillSelectionReceipt(
    val interactionId: AutofillInteractionId,
    val entryId: EntryId,
    val webScope: AutofillGrantContext? = null,
)

/** Closes a temporary Autofill unlock only after Android has received its result. */
@Singleton
internal class AutofillResultDelivery internal constructor(
    private val completeSelection: CompleteAutofillSelection,
    private val unlockLease: AutofillUnlockLease,
    private val interactionStore: AutofillInteractionStore,
    private val secureSessionAccessState: SecureSessionAccessState? = null,
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val continuations: TraceContinuationStore,
) {
    @Inject
    internal constructor(
        completeSelection: CompleteAutofillSelection,
        unlockLease: AutofillUnlockLease,
        interactionStore: AutofillInteractionStore,
        secureSessionAccessState: SecureSessionAccessState,
        logger: Logger,
        continuations: TraceContinuationStore,
    ) : this(
        completeSelection = completeSelection,
        unlockLease = unlockLease,
        interactionStore = interactionStore,
        secureSessionAccessState = secureSessionAccessState,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        logger = logger,
        continuations = continuations,
    )

    /** Commits the result to Android before releasing Autofill-owned access. */
    fun commitResult(
        receipt: AutofillSelectionReceipt,
        commitToPlatform: (markPlatformCommitted: () -> Unit) -> Unit,
    ) {
        val webScope = receipt.webScope?.normalized()
        var webDeliverySessionAtMs: Long? = null
        val webDeliveryClaimed =
            if (webScope != null) {
                val sessionAtMs =
                    secureSessionAccessState
                        ?.autofillSessionGeneration()
                        ?: error("Legacy web Autofill session is no longer authorized")
                webDeliverySessionAtMs = sessionAtMs
                val interaction = runBlocking { interactionStore.snapshot(receipt.interactionId) }
                check(
                    interaction?.scope == webScope &&
                        (interaction.webConsent as? AutofillWebConsent.Confirmed)?.sessionAtMs == sessionAtMs,
                ) {
                    "Legacy web Autofill interaction scope changed before delivery"
                }
                check(
                    runBlocking {
                        interactionStore.claimWebResultDelivery(
                            receipt.interactionId,
                            webScope,
                            receipt.entryId,
                            sessionAtMs,
                        )
                    },
                ) {
                    "Legacy web Autofill result is not confirmed for delivery"
                }
                true
            } else {
                false
            }
        var committed = false
        var platformCommitted = false
        var completionStarted = false
        var deliveryFailure: Throwable? = null
        var failureAfterPlatformCommit: Throwable? = null
        try {
            try {
                unlockLease.beginResultCompletion(receipt.interactionId)
                completionStarted = true
                runBlocking {
                    continuations.continueOrRoot(
                        receipt.interactionId.value,
                        Operations.AutofillResultDelivery,
                    ) {
                        val deliveryStartedAt = monotonicTimeMs()
                        logger.log(LogStatements.Autofill.resultDeliveryStarted())
                        try {
                            commitToPlatform {
                                check(!platformCommitted) { "Autofill platform result was marked committed more than once" }
                                platformCommitted = true
                            }
                            check(platformCommitted) { "Autofill platform callback returned without committing" }
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
                committed = true
            } catch (error: Throwable) {
                if (platformCommitted) {
                    committed = true
                    if (error !is Exception) failureAfterPlatformCommit = error
                } else {
                    deliveryFailure = error
                }
            }
        } finally {
            if (!committed) {
                if (webDeliveryClaimed && !platformCommitted) {
                    try {
                        runBlocking {
                            interactionStore.releaseWebResultDeliveryClaim(
                                receipt.interactionId,
                                requireNotNull(webScope),
                                receipt.entryId,
                                requireNotNull(webDeliverySessionAtMs),
                            )
                        }
                    } catch (cleanupFailure: Throwable) {
                        deliveryFailure = mergeFailure(deliveryFailure, cleanupFailure)
                    }
                }
                if (completionStarted) {
                    try {
                        runBlocking { unlockLease.endResultCompletion {} }
                    } catch (cleanupFailure: Throwable) {
                        deliveryFailure = mergeFailure(deliveryFailure, cleanupFailure)
                    }
                }
            }
        }
        if (committed) {
            try {
                completeDeliveredSelection(receipt)
            } catch (completionFailure: Throwable) {
                val primaryFailure = failureAfterPlatformCommit
                if (primaryFailure == null) throw completionFailure
                if (completionFailure !== primaryFailure) primaryFailure.addSuppressed(completionFailure)
            }
            failureAfterPlatformCommit?.let { throw it }
        } else {
            throw requireNotNull(deliveryFailure)
        }
    }

    private fun mergeFailure(
        primary: Throwable?,
        cleanupFailure: Throwable,
    ): Throwable {
        if (primary == null) return cleanupFailure
        if (primary === cleanupFailure) return primary
        if (cleanupFailure is Error && primary !is Error) {
            cleanupFailure.addSuppressed(primary)
            return cleanupFailure
        }
        primary.addSuppressed(cleanupFailure)
        return primary
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

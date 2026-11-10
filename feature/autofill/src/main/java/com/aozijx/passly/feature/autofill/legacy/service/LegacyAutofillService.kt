package com.aozijx.passly.feature.autofill.legacy.service

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.core.observability.AutofillLogSnapshot
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.interactionIdOrNull
import com.aozijx.passly.feature.autofill.legacy.LegacySaveFlow
import com.aozijx.passly.feature.autofill.legacy.LegacySavePreparation
import com.aozijx.passly.feature.autofill.legacy.service.adapter.LegacyPlatformAdapter
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyDiscoveryObservation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyInlinePresentationFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.AutofillStructureParser
import com.aozijx.passly.feature.autofill.runtime.launchAutofillServiceCleanup
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Legacy AutofillService Thin Adapter (API 31-33 fallback).
 */
@AndroidEntryPoint
class LegacyAutofillService : AutofillService() {
    @Inject
    internal lateinit var discoverCandidates: DiscoverAutofillCandidates

    @Inject
    internal lateinit var adapter: LegacyPlatformAdapter

    @Inject
    internal lateinit var diagnostics: LegacyAutofillDiagnostics

    @Inject
    internal lateinit var finishInteraction: FinishAutofillInteraction

    @Inject
    internal lateinit var legacySaveFlow: LegacySaveFlow

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var traceContinuations: TraceContinuationStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val job =
            serviceScope.launch {
                logger.withRootTrace(Operations.AutofillLegacyRequest) {
                    var discovery: LegacyAutofillDiagnostics.Discovery? = null
                    var pendingInteractionId: AutofillInteractionId? = null
                    try {
                        diagnostics.fillRequestReceived(request)
                        val parsed = AutofillStructureParser.parse(request.fillContexts)
                        diagnostics.structureParsed(
                            parsed,
                            request.fillContexts.size,
                            AutofillLogSnapshot.Legacy.Structure.Stage.FILL,
                        )
                        val internalRequest = adapter.buildRequest(parsed)
                        val discoveryStartedAt = monotonicTimeMs()
                        val discoveryLog = diagnostics.beginDiscovery(internalRequest.packageName)
                        discovery = discoveryLog
                        val outcome = discoverCandidates(internalRequest)
                        pendingInteractionId = outcome.interactionIdOrNull()
                        outcome.presentation?.let { mode ->
                            val inline = request.inlineSuggestionsRequest
                            diagnostics.presentationSelected(
                                mode,
                                mode == AutofillPresentation.SYSTEM_INLINE &&
                                    LegacyInlinePresentationFactory.isAvailable(inline),
                            )
                        }
                        val result =
                            adapter.buildResponseResult(
                                outcome = outcome,
                                parsed = parsed,
                                context = this@LegacyAutofillService,
                                inlineRequest = request.inlineSuggestionsRequest,
                            )
                        diagnostics.responseBuilt(internalRequest.packageName, result)
                        when (val observation = result.observation) {
                            is LegacyDiscoveryObservation.Completed -> {
                                traceContinuations.capture(observation.interactionId.value)
                                discoveryLog.completed(monotonicTimeMs() - discoveryStartedAt)
                            }

                            is LegacyDiscoveryObservation.Unavailable -> {
                                pendingInteractionId?.let { finishInteraction.cancel(it) }
                                pendingInteractionId = null
                                discoveryLog.unavailable(observation.reason)
                            }
                        }
                        callback.onSuccess(result.response)
                    } catch (e: CancellationException) {
                        try {
                            withContext(NonCancellable) {
                                pendingInteractionId?.let { finishInteraction.cancel(it) }
                            }
                        } catch (cleanupFailure: Exception) {
                            e.addSuppressed(cleanupFailure)
                        }
                        logger.log(LogStatements.Autofill.requestCancelled(AutofillInteractionSource.LEGACY))
                        throw e
                    } catch (e: Exception) {
                        pendingInteractionId?.let { interactionId ->
                            try {
                                withContext(NonCancellable) { finishInteraction.cancel(interactionId) }
                            } catch (cleanupFailure: Exception) {
                                e.addSuppressed(cleanupFailure)
                            }
                        }
                        diagnostics.discoveryFailed(e, discovery)
                        callback.onFailure("Fill request failed")
                    }
                }
            }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        val completion = AtomicBoolean(false)
        serviceScope.launch {
            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                diagnostics.saveRequestReceived(request)
                diagnostics.saveStarted()
                try {
                    val parsed = AutofillStructureParser.parse(request.fillContexts)
                    diagnostics.structureParsed(
                        parsed,
                        request.fillContexts.size,
                        AutofillLogSnapshot.Legacy.Structure.Stage.SAVE,
                    )
                    when (val result = legacySaveFlow.prepare(adapter.buildRequest(parsed))) {
                        LegacySavePreparation.Saved -> {
                            diagnostics.saveCompleted()
                            if (completion.compareAndSet(false, true)) callback.onSuccess()
                        }

                        LegacySavePreparation.Queued -> {
                            diagnostics.saveStaged()
                            if (completion.compareAndSet(false, true)) callback.onSuccess()
                        }

                        LegacySavePreparation.Disabled -> {
                            failSave(
                                callback,
                                completion,
                                "Credential saving is disabled",
                                Reasons.SessionRestricted,
                            )
                        }

                        LegacySavePreparation.InvalidDraft -> {
                            failSave(
                                callback,
                                completion,
                                "No password was available to save",
                                Reasons.NoFillableFields,
                            )
                        }

                        LegacySavePreparation.Failed -> {
                            failSave(
                                callback,
                                completion,
                                "Credential could not be saved",
                                Reasons.StorageFailure,
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    failSave(callback, completion, "Credential saving was cancelled", Reasons.UnknownFailure)
                    throw cancelled
                } catch (error: Exception) {
                    diagnostics.saveFailed(error)
                    if (completion.compareAndSet(false, true)) {
                        callback.onFailure("Credential could not be saved")
                    }
                }
            }
        }
    }

    private fun failSave(
        callback: SaveCallback,
        completion: AtomicBoolean,
        message: String,
        reason: ReasonCode,
    ) {
        diagnostics.saveUnavailable(reason)
        if (completion.compareAndSet(false, true)) callback.onFailure(message)
    }

    override fun onDisconnected() {
        // Android disconnects the service immediately after delivering FillResponse,
        // before the user selects its authenticated dataset. Keep those interactions
        // alive for the response PendingIntent; AutofillInteractionTimeout owns expiry.
        super.onDisconnected()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

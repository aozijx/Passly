package com.aozijx.passly.feature.autofill.legacy.service

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.launchAutofillServiceCleanup
import com.aozijx.passly.feature.autofill.legacy.LegacySaveFlow
import com.aozijx.passly.feature.autofill.legacy.LegacySavePreparation
import com.aozijx.passly.feature.autofill.legacy.service.adapter.LegacyPlatformAdapter
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyDiscoveryObservation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyInlinePresentationFactory
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.parser.AutofillStructureParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Legacy AutofillService Thin Adapter (API 31-33 fallback).
 */
@AndroidEntryPoint
class LegacyAutofillService : AutofillService() {

    @Inject
    lateinit var discoverCandidates: DiscoverAutofillCandidates

    @Inject
    lateinit var adapter: LegacyPlatformAdapter

    @Inject
    lateinit var finishInteraction: FinishAutofillInteraction

    @Inject
    lateinit var legacySaveFlow: LegacySaveFlow

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var traceContinuations: TraceContinuationStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val job = serviceScope.launch {
            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                var requestFields = LogFields.Empty
                try {
                    val parsed = AutofillStructureParser.parse(request.fillContexts)
                    val internalRequest = adapter.buildRequest(parsed)
                    requestFields = requestFields(internalRequest.packageName)
                    val discoveryStartedAt = monotonicTimeMs()
                    logger.log(LogStatements.Autofill.discoveryStarted(requestFields))
                    val outcome = discoverCandidates(internalRequest)
                    val presentation = when (outcome) {
                        is AutofillDiscoveryOutcome.Candidates -> outcome.context.presentation
                        is AutofillDiscoveryOutcome.NoCandidates -> outcome.context.presentation
                        is AutofillDiscoveryOutcome.RequiresUnlock -> outcome.context.presentation
                        else -> null
                    }
                    presentation?.let { mode ->
                        val inline = request.inlineSuggestionsRequest
                        logger.log(LogStatements.Autofill.presentationSelected(
                            mode,
                            mode == AutofillPresentation.SYSTEM_INLINE &&
                                LegacyInlinePresentationFactory.isAvailable(inline),
                        ))
                    }
                    val result = adapter.buildResponseResult(
                        outcome = outcome,
                        parsed = parsed,
                        context = this@LegacyAutofillService,
                        inlineRequest = request.inlineSuggestionsRequest,
                    )
                    when (val observation = result.observation) {
                        is LegacyDiscoveryObservation.Completed -> {
                            traceContinuations.capture(observation.interactionId.value)
                            logger.log(
                                LogStatements.Autofill.discoveryCompleted(
                                    monotonicTimeMs() - discoveryStartedAt,
                                    requestFields,
                                ),
                            )
                        }
                        is LegacyDiscoveryObservation.Unavailable ->
                            logDiscoveryUnavailable(observation.reason, requestFields)
                    }
                    callback.onSuccess(result.response)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.log(LogStatements.Autofill.discoveryFailed(e, fields = requestFields))
                    callback.onFailure("Fill request failed")
                }
            }
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val completion = AtomicBoolean(false)
        serviceScope.launch {
            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                logger.log(LogStatements.Autofill.saveStarted())
                try {
                    val parsed = AutofillStructureParser.parse(request.fillContexts)
                    when (val result = legacySaveFlow.prepare(adapter.buildRequest(parsed))) {
                    LegacySavePreparation.Saved -> {
                        logger.log(LogStatements.Autofill.saveCompleted())
                        if (completion.compareAndSet(false, true)) callback.onSuccess()
                    }

                    LegacySavePreparation.Queued -> {
                        logger.log(LogStatements.Autofill.saveStaged())
                        if (completion.compareAndSet(false, true)) callback.onSuccess()
                    }

                        LegacySavePreparation.Disabled -> failSave(
                            callback, completion, "Credential saving is disabled", Reasons.SessionRestricted,
                        )
                        LegacySavePreparation.InvalidDraft -> failSave(
                            callback, completion, "No password was available to save", Reasons.NoFillableFields,
                        )
                        LegacySavePreparation.Failed -> failSave(
                            callback, completion, "Credential could not be saved", Reasons.StorageFailure,
                        )
                    }
                } catch (cancelled: CancellationException) {
                    failSave(callback, completion, "Credential saving was cancelled", Reasons.UnknownFailure)
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.saveFailed(error))
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
        logger.log(LogStatements.Autofill.saveUnavailable(reason))
        if (completion.compareAndSet(false, true)) callback.onFailure(message)
    }

    private fun logDiscoveryUnavailable(reason: ReasonCode, logFields: LogFields) {
        logger.log(LogStatements.Autofill.discoveryUnavailable(reason, logFields))
    }

    private fun requestFields(packageName: String): LogFields =
        LogStatements.Autofill.context(AutofillInteractionSource.LEGACY, packageName)

    override fun onDisconnected() {
        launchAutofillServiceCleanup(serviceScope, cleanup = {
            finishInteraction.expireAll(AutofillInteractionSource.LEGACY)
        })
        super.onDisconnected()
    }

    override fun onDestroy() {
        launchAutofillServiceCleanup(serviceScope, cleanup = {
            finishInteraction.expireAll(AutofillInteractionSource.LEGACY)
        })
        serviceScope.cancel()
        super.onDestroy()
    }
}

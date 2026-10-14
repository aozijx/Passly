package com.aozijx.passly.feature.autofill.legacy.service

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.legacy.LegacySaveFlow
import com.aozijx.passly.feature.autofill.legacy.LegacySavePendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.LegacySavePreparation
import com.aozijx.passly.feature.autofill.legacy.service.adapter.LegacyPlatformAdapter
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
    lateinit var legacySavePendingIntentFactory: LegacySavePendingIntentFactory

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        TelemetryRuntime.i(EventCategory.AUTOFILL, "legacy.service_created")
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val parsed = AutofillStructureParser.parse(request.fillContexts)
        val internalRequest = adapter.buildRequest(parsed)

        val job = serviceScope.launch {
            try {
                val outcome = discoverCandidates(internalRequest)
                val fillResponse = adapter.buildResponse(
                    outcome = outcome,
                    parsed = parsed,
                    context = this@LegacyAutofillService,
                )
                callback.onSuccess(fillResponse)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TelemetryRuntime.e(EventCategory.AUTOFILL, "legacy.fill_request_failed", throwable = e)
                callback.onFailure(e.message ?: "Fill request failed")
            }
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val completion = AtomicBoolean(false)
        serviceScope.launch {
            try {
                val parsed = AutofillStructureParser.parse(request.fillContexts)
                when (val result = legacySaveFlow.prepare(adapter.buildRequest(parsed))) {
                    LegacySavePreparation.Saved -> {
                        if (completion.compareAndSet(false, true)) callback.onSuccess()
                    }

                    is LegacySavePreparation.Staged -> {
                        val pendingIntent = legacySavePendingIntentFactory.create(
                            this@LegacyAutofillService,
                            result.token,
                        )
                        if (completion.compareAndSet(false, true)) {
                            callback.onSuccess(pendingIntent.intentSender)
                        }
                    }

                    LegacySavePreparation.Disabled -> failSave(
                        callback,
                        completion,
                        "Credential saving is disabled",
                    )

                    LegacySavePreparation.InvalidDraft -> failSave(
                        callback,
                        completion,
                        "No password was available to save",
                    )

                    LegacySavePreparation.Failed -> failSave(
                        callback,
                        completion,
                        "Credential could not be saved",
                    )
                }
            } catch (cancelled: CancellationException) {
                failSave(callback, completion, "Credential saving was cancelled")
                throw cancelled
            } catch (_: Exception) {
                TelemetryRuntime.e(EventCategory.AUTOFILL, "legacy.save_request_failed")
                failSave(callback, completion, "Credential could not be saved")
            }
        }
    }

    private fun failSave(
        callback: SaveCallback,
        completion: AtomicBoolean,
        message: String,
    ) {
        if (completion.compareAndSet(false, true)) callback.onFailure(message)
    }

    override fun onDisconnected() {
        serviceScope.launch {
            finishInteraction.expireAll(AutofillInteractionSource.LEGACY)
        }
        super.onDisconnected()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

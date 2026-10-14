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
        callback.onFailure("Legacy Autofill save is disabled")
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

package com.aozijx.passly.feature.autofill.legacy.service

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSaveSource
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.internal.di.Heuristic
import com.aozijx.passly.feature.autofill.internal.save.SaveRequestAnalyzer
import com.aozijx.passly.feature.autofill.legacy.service.adapter.LegacyPlatformAdapter
import com.aozijx.passly.feature.autofill.legacy.service.parser.AutofillStructureParser
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
    @Heuristic
    lateinit var strategy: FieldMatchStrategy

    @Inject
    lateinit var adapter: LegacyPlatformAdapter

    @Inject
    lateinit var saveCredential: SaveAutofillCredential

    @Inject
    lateinit var saveAnalyzer: SaveRequestAnalyzer

    @Inject
    lateinit var settingsSource: InteractionSettingsSource

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

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        val parsed = AutofillStructureParser.parse(request.fillContexts)
        val internalRequest = adapter.buildRequest(parsed)

        serviceScope.launch {
            try {
                val settings = settingsSource.interaction.first().autofill
                val pending = saveAnalyzer.buildCandidate(
                    parsed = parsed,
                    request = internalRequest,
                    strategy = strategy,
                    settings = settings,
                )
                if (pending == null) {
                    callback.onSuccess()
                    return@launch
                }

                val saveOutcome = saveCredential(
                    draft = AutofillSaveDraft(
                        packageName = pending.packageName.orEmpty(),
                        webDomain = pending.webDomain,
                        pageTitle = pending.pageTitle,
                        username = pending.username,
                        password = pending.password,
                    ),
                    source = AutofillSaveSource.LEGACY_PROMPT,
                )
                if (saveOutcome !is AutofillSaveOutcome.Saved) {
                    callback.onFailure("Credential could not be saved")
                    return@launch
                }

                TelemetryRuntime.i(EventCategory.AUTOFILL, "legacy.credential_saved")
                callback.onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TelemetryRuntime.e(EventCategory.AUTOFILL, "legacy.save_request_failed", throwable = e)
                callback.onFailure(e.message ?: "Save request failed")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

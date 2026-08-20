package com.aozijx.passly.feature.autofill.legacy.service

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.autofill.internal.FillRequestDispatcher
import com.aozijx.passly.feature.autofill.internal.di.Heuristic
import com.aozijx.passly.feature.autofill.internal.save.SaveRequestAnalyzer
import com.aozijx.passly.feature.autofill.legacy.service.adapter.LegacyPlatformAdapter
import com.aozijx.passly.feature.autofill.legacy.service.parser.AutofillStructureParser
import com.aozijx.passly.feature.autofill.shared.SaveAutofillCredentialUseCase
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
    @Heuristic
    lateinit var dispatcher: FillRequestDispatcher

    @Inject
    @Heuristic
    lateinit var strategy: FieldMatchStrategy

    @Inject
    lateinit var adapter: LegacyPlatformAdapter

    @Inject
    lateinit var saveCredential: SaveAutofillCredentialUseCase

    @Inject
    lateinit var saveAnalyzer: SaveRequestAnalyzer

    @Inject
    lateinit var settingsRepository: AppSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppTelemetry.i("LegacyAutofill", "Service created (API < 34 fallback)")
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
                val response = dispatcher.dispatch(internalRequest)
                val fillResponse = adapter.buildResponse(
                    response = response,
                    parsed = parsed,
                    uiMode = response.presentation,
                    context = this@LegacyAutofillService,
                )
                callback.onSuccess(fillResponse)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e("LegacyAutofill", "Fill request failed", e)
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
                val settings = settingsRepository.settings.first().interaction.autofill
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

                saveCredential(
                    packageName = pending.packageName,
                    webDomain = pending.webDomain,
                    pageTitle = pending.pageTitle,
                    usernameValue = pending.username,
                    passwordValue = pending.password,
                )

                AppTelemetry.i("LegacyAutofill", "Credential saved successfully")
                callback.onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e("LegacyAutofill", "Save request failed", e)
                callback.onFailure(e.message ?: "Save request failed")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

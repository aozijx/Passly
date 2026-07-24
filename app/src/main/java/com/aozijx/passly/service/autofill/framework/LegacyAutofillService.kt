package com.aozijx.passly.service.autofill.framework

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.app.di.Heuristic
import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.autofill.usecase.AutofillUseCases
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.service.autofill.framework.adapter.LegacyPlatformAdapter
import com.aozijx.passly.service.autofill.framework.parser.AutofillStructureParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 传统 AutofillService 薄适配器（API 31-33 兜底）。
 *
 * 职责仅限于：
 * AssessStructure → [LegacyPlatformAdapter] → [FillRequestDispatcher] → [LegacyPlatformAdapter] → FillResponse。
 * 严禁在此类中编写任何业务判断逻辑。
 */
@AndroidEntryPoint
class LegacyAutofillService : AutofillService() {

    @Inject
    @Heuristic
    lateinit var dispatcher: FillRequestDispatcher
    @Inject
    lateinit var adapter: LegacyPlatformAdapter
    @Inject
    lateinit var useCases: AutofillUseCases

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppLog.i("LegacyAutofill", "Service created (API < 34 fallback)")
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val parsed = AutofillStructureParser.parse(request.fillContexts)
        val internalRequest = adapter.buildRequest(parsed)

        serviceScope.launch {
            val response = dispatcher.dispatch(internalRequest)
            val fillResponse = adapter.buildResponse(
                response = response,
                parsed = parsed,
                uiMode = AutofillUiMode.SYSTEM_INLINE,
                context = this@LegacyAutofillService,
            )
            callback.onSuccess(fillResponse)
        }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        val parsed = AutofillStructureParser.parse(request.fillContexts)

        serviceScope.launch {
            val result = useCases.saveCredential(
                packageName = parsed.packageName,
                webDomain = parsed.webDomain,
                pageTitle = parsed.pageTitle,
                usernameValue = parsed.usernameValue ?: "",
                passwordValue = parsed.passwordValue ?: "",
            )
            when (result) {
                is AppResult.Success -> {
                    AppLog.i("LegacyAutofill", "Credential saved successfully")
                    callback.onSuccess()
                }

                is AppResult.Failure -> {
                    AppLog.e("LegacyAutofill", "Save failed: ${result.error.message}")
                    callback.onFailure(result.error.message)
                }
            }
        }
    }
}

@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.aozijx.passly.app.diagnostics.AppTelemetry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modern CredentialProviderService 薄适配器（API 34+）。
 *
 * 职责仅限于：
 * BeginGetCredentialRequest → [CredentialBeginGetHandler] → BeginGetCredentialResponse。
 * 严禁在此类中编写任何业务判断逻辑。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class ModernCredentialService : CredentialProviderService() {

    @Inject
    lateinit var beginGetHandler: CredentialBeginGetHandler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ModernCred"
        const val ACTION_GET_PASSWORD = "com.aozijx.passly.ACTION_GET_PASSWORD"
        const val ACTION_UNLOCK = "com.aozijx.passly.ACTION_CREDENTIAL_UNLOCK"

        const val EXTRA_CREDENTIAL_DATA = "credential_data"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        val job = scope.launch {
            try {
                val response = beginGetHandler.resolve(request, this@ModernCredentialService)
                callback.onResult(response)
            } catch (e: CancellationException) {
                // Android cancelled the request; no callback is allowed after cancellation.
                throw e
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Credential request failed", e)
                callback.onError(GetCredentialUnknownException(e.message ?: "Unknown error"))
            }
        }
        cancellationSignal.setOnCancelListener(job::cancel)
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, androidx.credentials.exceptions.CreateCredentialException>
    ) {
        callback.onResult(BeginCreateCredentialResponse(emptyList()))
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, androidx.credentials.exceptions.ClearCredentialException>
    ) {
        callback.onResult(null)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

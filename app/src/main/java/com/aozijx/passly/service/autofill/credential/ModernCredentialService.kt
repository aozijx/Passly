@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.di.Strict
import com.aozijx.passly.security.session.SessionStateProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modern CredentialProviderService 薄适配器（API 34+）。
 *
 * 职责仅限于：
 * BeginGetCredentialRequest → [CredentialPlatformAdapter] → [FillRequestDispatcher]
 * → [CredentialPlatformAdapter] → BeginGetCredentialResponse。
 * 严禁在此类中编写任何业务判断逻辑。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class ModernCredentialService : CredentialProviderService() {

    @Inject
    @Strict
    lateinit var dispatcher: FillRequestDispatcher
    @Inject
    lateinit var sessionState: SessionStateProvider
    @Inject
    lateinit var adapter: CredentialPlatformAdapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ModernCred"
        const val ACTION_GET_PASSWORD = "com.aozijx.passly.ACTION_GET_PASSWORD"
        const val ACTION_GET_PASSKEY = "com.aozijx.passly.ACTION_GET_PASSKEY"
        const val ACTION_UNLOCK = "com.aozijx.passly.ACTION_CREDENTIAL_UNLOCK"

        const val EXTRA_CREDENTIAL_DATA = "credential_data"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_ENTRY_TITLE = "entry_title"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_WEB_DOMAIN = "web_domain"

        private const val UNLOCK_TITLE = "Vault is locked — tap to unlock"
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        try {
            if (sessionState.isLocked()) {
                callback.onResult(
                    BeginGetCredentialResponse(
                        authenticationActions = listOf(
                            AuthenticationAction(
                                UNLOCK_TITLE,
                                CredentialPendingIntentFactory.createUnlockPendingIntent(this)
                            )
                        )
                    )
                )
                return
            }

            scope.launch {
                val response = processGetCredentialRequest(request)
                callback.onResult(response)
            }
        } catch (e: Exception) {
            Logcat.e(TAG, "onBeginGetCredentialRequest failed", e)
            callback.onError(GetCredentialUnknownException(e.message ?: "Unknown error"))
        }
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

    private fun processGetCredentialRequest(request: BeginGetCredentialRequest): BeginGetCredentialResponse {
        val packageName = request.callingAppInfo?.packageName ?: ""
        val entries = mutableListOf<CredentialEntry>()

        for (option in request.beginGetCredentialOptions) {
            val internalRequest = adapter.buildRequest(packageName)
            val response = dispatcher.dispatch(internalRequest)

            when (option) {
                is BeginGetPasswordOption -> entries.addAll(
                    adapter.buildPasswordEntries(response, this, packageName, option)
                )

                is BeginGetPublicKeyCredentialOption -> entries.addAll(
                    adapter.buildPasskeyEntries(response, this, packageName, option)
                )
            }
        }

        Logcat.i(TAG, "Phase 1: ${entries.size} entries for $packageName")
        return BeginGetCredentialResponse(entries)
    }
}
package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialEntry
import com.aozijx.passly.R
import com.aozijx.passly.domain.autofill.model.AutofillStatus
import com.aozijx.passly.feature.autofill.internal.FillRequestDispatcher
import com.aozijx.passly.feature.autofill.internal.di.Strict
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared Credential Manager phase-one handler.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialBeginGetHandler @Inject constructor(
    @param:Strict private val dispatcher: FillRequestDispatcher,
    private val adapter: CredentialPlatformAdapter,
    private val pendingIntentFactory: CredentialPendingIntentFactory,
) {
    suspend fun resolve(
        request: BeginGetCredentialRequest,
        context: Context,
        includeUnlockAction: Boolean = true,
    ): BeginGetCredentialResponse {
        val passwordOptions = request.beginGetCredentialOptions
            .filterIsInstance<BeginGetPasswordOption>()
        if (passwordOptions.isEmpty()) return BeginGetCredentialResponse()

        val packageName =
            CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo)
                ?: return BeginGetCredentialResponse()
        val response = dispatcher.dispatch(adapter.buildRequest(packageName))
        if (response.status == AutofillStatus.LOCKED) {
            return if (includeUnlockAction) {
                BeginGetCredentialResponse(
                    authenticationActions = listOf(
                        AuthenticationAction(
                            context.getString(R.string.vault_locked_title),
                            pendingIntentFactory.createUnlockPendingIntent(context),
                        )
                    )
                )
            } else {
                BeginGetCredentialResponse()
            }
        }

        val entries = mutableListOf<CredentialEntry>()
        for (option in passwordOptions) {
            entries += adapter.buildPasswordEntries(
                response,
                context,
                option,
            )
        }
        return BeginGetCredentialResponse(entries)
    }
}

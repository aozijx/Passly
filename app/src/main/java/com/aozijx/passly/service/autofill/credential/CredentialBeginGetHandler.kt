@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialEntry
import com.aozijx.passly.R
import com.aozijx.passly.app.di.Strict
import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.model.FillAvailability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared Credential Manager phase-one handler.
 *
 * It is reused by the provider service and by the authentication action after
 * unlocking, so both paths apply exactly the same query and settings policy.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialBeginGetHandler @Inject constructor(
    @param:Strict private val dispatcher: FillRequestDispatcher,
    private val adapter: CredentialPlatformAdapter,
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
        if (response.availability == FillAvailability.LOCKED) {
            return if (includeUnlockAction) {
                BeginGetCredentialResponse(
                    authenticationActions = listOf(
                        AuthenticationAction(
                            context.getString(R.string.vault_locked_title),
                            CredentialPendingIntentFactory.createUnlockPendingIntent(context),
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

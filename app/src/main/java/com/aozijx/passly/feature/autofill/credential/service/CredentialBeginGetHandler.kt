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
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.feature.autofill.credential.CredentialManagerFlow
import com.aozijx.passly.feature.autofill.credential.CredentialManagerUnlockResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared Credential Manager phase-one handler.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialBeginGetHandler @Inject constructor(
    private val flow: CredentialManagerFlow,
    private val adapter: CredentialPlatformAdapter,
    private val pendingIntentFactory: CredentialPendingIntentFactory,
) {
    suspend fun resolve(
        request: BeginGetCredentialRequest,
        context: Context,
    ): BeginGetCredentialResponse {
        val passwordOptions = request.beginGetCredentialOptions
            .filterIsInstance<BeginGetPasswordOption>()
        if (passwordOptions.isEmpty()) return BeginGetCredentialResponse()

        val packageName =
            CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo)
                ?: return BeginGetCredentialResponse()
        return buildResponse(
            discovery = flow.beginGet(adapter.buildRequest(packageName)),
            passwordOptions = passwordOptions,
            context = context,
            includeUnlockAction = true,
        )
    }

    suspend fun resolveAfterUnlock(
        request: BeginGetCredentialRequest,
        interactionId: AutofillInteractionId,
        context: Context,
    ): CredentialUnlockResponse {
        val passwordOptions = request.beginGetCredentialOptions
            .filterIsInstance<BeginGetPasswordOption>()
        if (passwordOptions.isEmpty()) return CredentialUnlockResponse.Unavailable
        val packageName = CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo)
            ?: return CredentialUnlockResponse.Unavailable

        return when (
            val result = flow.unlockAndRediscover(
                interactionId,
                adapter.buildRequest(packageName),
            )
        ) {
            is CredentialManagerUnlockResult.NotAuthorized -> {
                CredentialUnlockResponse.NotAuthorized(result)
            }

            is CredentialManagerUnlockResult.Rediscovered -> CredentialUnlockResponse.Ready(
                buildResponse(
                    discovery = result.discovery,
                    passwordOptions = passwordOptions,
                    context = context,
                    includeUnlockAction = false,
                ),
            )
        }
    }

    private suspend fun buildResponse(
        discovery: AutofillDiscoveryOutcome,
        passwordOptions: List<BeginGetPasswordOption>,
        context: Context,
        includeUnlockAction: Boolean,
    ): BeginGetCredentialResponse = when (discovery) {
        is AutofillDiscoveryOutcome.RequiresUnlock -> if (includeUnlockAction) {
                BeginGetCredentialResponse(
                    authenticationActions = listOf(
                        AuthenticationAction(
                            context.getString(R.string.vault_locked_title),
                            pendingIntentFactory.createUnlockPendingIntent(
                                context,
                                discovery.snapshot.id,
                            ),
                        )
                    )
                )
            } else {
                flow.cancel(discovery.snapshot.id)
                BeginGetCredentialResponse()
            }

        is AutofillDiscoveryOutcome.Candidates -> {
            val entries = mutableListOf<CredentialEntry>()
            for (option in passwordOptions) {
                entries += adapter.buildPasswordEntries(
                    interactionId = discovery.snapshot.id,
                    candidates = discovery.candidates,
                    context = context,
                    option = option,
                )
            }
            if (entries.isEmpty()) {
                flow.cancel(discovery.snapshot.id)
                BeginGetCredentialResponse()
            } else {
                BeginGetCredentialResponse(entries)
            }
        }

        is AutofillDiscoveryOutcome.NoCandidates -> {
            flow.cancel(discovery.snapshot.id)
            BeginGetCredentialResponse()
        }

        AutofillDiscoveryOutcome.Disabled,
        AutofillDiscoveryOutcome.UnsupportedFields,
        is AutofillDiscoveryOutcome.Failed,
        -> BeginGetCredentialResponse()
    }
}

sealed interface CredentialUnlockResponse {
    data class Ready(val response: BeginGetCredentialResponse) : CredentialUnlockResponse
    data class NotAuthorized(
        val result: CredentialManagerUnlockResult.NotAuthorized,
    ) : CredentialUnlockResponse
    data object Unavailable : CredentialUnlockResponse
}

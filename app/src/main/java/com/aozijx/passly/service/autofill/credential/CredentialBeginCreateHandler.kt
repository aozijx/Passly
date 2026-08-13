@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.CreateEntry
import com.aozijx.passly.R
import com.aozijx.passly.data.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Manager password-create query phase.
 *
 * Passkey requests are deliberately rejected until Passly owns an encrypted
 * signing key and can produce and verify complete WebAuthn payloads.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialBeginCreateHandler @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
) {
    suspend fun resolve(
        request: BeginCreateCredentialRequest,
        context: Context,
    ): BeginCreateCredentialResponse {
        if (request !is BeginCreatePasswordCredentialRequest) {
            throw CreateCredentialUnsupportedException(
                "Passly currently supports password creation only"
            )
        }

        val policy = settingsRepository.settings.first().interaction.autofill
        if (!policy.enabled || !policy.credentialManagerEnabled) return BeginCreateCredentialResponse()

        if (CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo) == null) {
            return BeginCreateCredentialResponse()
        }

        val entry = CreateEntry.Builder(
            context.getString(R.string.app_name),
            CredentialPendingIntentFactory.createPasswordCreatePendingIntent(context),
        )
            .setDescription(context.getString(R.string.credential_create_password_description))
            .setIcon(Icon.createWithResource(context, R.mipmap.launcher_logo))
            // Password creation must remain an explicit user selection.
            .setAutoSelectAllowed(false)
            .build()

        return BeginCreateCredentialResponse(createEntries = listOf(entry))
    }
}

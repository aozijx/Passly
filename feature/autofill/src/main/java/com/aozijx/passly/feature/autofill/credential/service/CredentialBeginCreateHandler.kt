package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.CreateEntry
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.settings.port.AutofillSettingsSource
import com.aozijx.passly.feature.autofill.R
import com.aozijx.passly.feature.autofill.credential.CredentialManagerFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Credential Manager password-create query phase.
 *
 * Passkey requests are deliberately rejected until Passly owns an encrypted
 * signing key and can produce and verify complete WebAuthn payloads.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
internal class CredentialBeginCreateHandler
    @Inject
    constructor(
        private val settingsSource: AutofillSettingsSource,
        private val flow: CredentialManagerFlow,
        private val pendingIntentFactory: CredentialPendingIntentFactory,
        private val logger: Logger,
        private val traceContinuations: TraceContinuationStore,
    ) {
        suspend fun resolve(
            request: BeginCreateCredentialRequest,
            context: Context,
        ): BeginCreateCredentialResponse {
            if (request !is BeginCreatePasswordCredentialRequest) {
                throw CreateCredentialUnsupportedException(
                    "Passly currently supports password creation only",
                )
            }

            val policy = settingsSource.autofillSettings.first()
            if (
                !policy.enabled ||
                !policy.credentialManagerEnabled
            ) {
                return BeginCreateCredentialResponse()
            }

            val packageName = CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo)
            if (packageName == null) {
                return BeginCreateCredentialResponse()
            }
            val interaction = flow.beginCreate(AutofillGrantContext(packageName, null))
            traceContinuations.capture(interaction.id.value)

            val entryBuilder =
                CreateEntry
                    .Builder(
                        context.getString(R.string.app_name),
                        pendingIntentFactory.createPasswordCreatePendingIntent(context, interaction.id),
                    ).setDescription(context.getString(R.string.credential_create_password_description))
            resolveCallingAppIcon(context, packageName)?.let(entryBuilder::setIcon)
            val entry =
                entryBuilder
                    // Password creation must remain an explicit user selection.
                    .setAutoSelectAllowed(false)
                    .build()

            logger.log(LogStatements.Autofill.saveStaged())
            return BeginCreateCredentialResponse(createEntries = listOf(entry))
        }

        private fun resolveCallingAppIcon(
            context: Context,
            packageName: String,
        ): Icon? {
            val iconResourceId =
                try {
                    context.packageManager.getApplicationInfo(packageName, 0).icon
                } catch (_: Exception) {
                    return null
                }
            if (iconResourceId == 0) return null
            return try {
                Icon.createWithResource(packageName, iconResourceId)
            } catch (_: Exception) {
                null
            }
        }
    }

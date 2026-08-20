package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillResponse
import com.aozijx.passly.domain.autofill.model.AutofillSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Platform Adapter: Converts between Android CredentialManager API and core models.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialPlatformAdapter @Inject constructor() {

    companion object {
        private const val TAG = "CredAdapter"
    }

    fun buildRequest(packageName: String): AutofillRequest {
        return AutofillRequest(
            packageName = packageName,
            domain = null,
            fields = listOf(
                AutofillField(
                    id = "credential_username",
                    hints = setOf("USERNAME"),
                    inputType = null,
                    isFocused = true
                ),
                AutofillField(
                    id = "credential_password",
                    hints = setOf("PASSWORD"),
                    inputType = null,
                    isFocused = false
                ),
            ),
            source = AutofillSource.CREDENTIAL_MANAGER,
        )
    }

    fun buildPasswordEntries(
        response: AutofillResponse,
        context: Context,
        option: BeginGetPasswordOption,
    ): List<PasswordCredentialEntry> {
        val candidates = response.candidates.filter {
            it.entry.username.isNotBlank() &&
                    (option.allowedUserIds.isEmpty() || it.entry.username in option.allowedUserIds)
        }
        if (candidates.isEmpty()) {
            AppTelemetry.d(TAG, "No candidates for password entries")
            return emptyList()
        }

        return candidates.map { candidate ->
            CredentialEntryFactory.buildPasswordEntry(
                context = context,
                candidate = candidate,
                option = option,
            )
        }
    }
}

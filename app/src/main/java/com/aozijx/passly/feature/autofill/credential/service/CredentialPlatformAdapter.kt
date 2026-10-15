package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Platform Adapter: Converts between Android CredentialManager API and core models.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialPlatformAdapter @Inject constructor(
    private val entryFactory: CredentialEntryFactory,
) {

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
        interactionId: AutofillInteractionId,
        candidates: List<AutofillCandidateSummary>,
        context: Context,
        option: BeginGetPasswordOption,
    ): List<PasswordCredentialEntry> {
        val eligibleCandidates = candidates.filter {
            it.entry.username.isNotBlank() &&
                    (option.allowedUserIds.isEmpty() || it.entry.username in option.allowedUserIds)
        }
        if (eligibleCandidates.isEmpty()) {
            return emptyList()
        }

        return eligibleCandidates.map { candidate ->
            entryFactory.buildPasswordEntry(
                context = context,
                interactionId = interactionId,
                candidate = candidate,
                option = option,
            )
        }
    }
}

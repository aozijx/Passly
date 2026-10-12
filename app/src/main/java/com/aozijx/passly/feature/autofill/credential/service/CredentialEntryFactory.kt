package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Entry Factory: Builds entry objects for Credential Manager.
 */
@Singleton
class CredentialEntryFactory @Inject constructor(
    private val pendingIntentFactory: CredentialPendingIntentFactory,
) {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun buildPasswordEntry(
        context: Context,
        interactionId: AutofillInteractionId,
        candidate: AutofillCandidateSummary,
        option: BeginGetPasswordOption,
    ): PasswordCredentialEntry = PasswordCredentialEntry.Builder(
        context = context,
        username = candidate.entry.username,
        pendingIntent = pendingIntentFactory.createPasswordGetPendingIntent(
            context = context,
            interactionId = interactionId,
            entryId = candidate.entry.id,
        ),
        beginGetPasswordOption = option,
    )
        .setDisplayName(candidate.entry.title)
        .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_lock))
        .setLastUsedTime(null)
        .setAutoSelectAllowed(false)
        .setDefaultIconPreferredAsSingleProvider(false)
        .build()
}

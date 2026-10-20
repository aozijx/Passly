package com.aozijx.passly.presentation.feature.autofill.legacy

import android.content.Intent
import android.view.autofill.AutofillId
import androidx.core.content.IntentCompat
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras

internal fun Intent.toAutofillFillRequest(): AutofillFillRequest? {
    val interactionId = getStringExtra(AutofillLaunchExtras.INTERACTION_ID)
        ?.takeIf(String::isNotBlank)
        ?.let(::AutofillInteractionId)
        ?: return null
    val packageName = getStringExtra(AutofillLaunchExtras.PACKAGE_NAME)
        ?.takeIf(String::isNotBlank)
        ?: return null
    val uiMode = getStringExtra(AutofillLaunchExtras.UI_MODE)
        ?.let { raw -> AutofillPresentation.entries.firstOrNull { it.name == raw } }
        ?: AutofillPresentation.SYSTEM_INLINE

    return AutofillFillRequest(
        interactionId = interactionId,
        uiMode = uiMode,
        packageName = packageName,
        webDomain = getStringExtra(AutofillLaunchExtras.WEB_DOMAIN),
        directEntryId = getStringExtra(AutofillLaunchExtras.ENTRY_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::EntryId),
        returnsDataset = getBooleanExtra(AutofillLaunchExtras.RETURN_DATASET, false),
        editableIds = IntentCompat.getParcelableArrayExtra(
            this,
            AutofillLaunchExtras.EDITABLE_IDS,
            AutofillId::class.java,
        )?.filterIsInstance<AutofillId>().orEmpty(),
        usernameIds = autofillIds(AutofillLaunchExtras.USERNAME_IDS),
        passwordIds = autofillIds(AutofillLaunchExtras.PASSWORD_IDS),
        otpIds = autofillIds(AutofillLaunchExtras.OTP_IDS),
    )
}

private fun Intent.autofillIds(key: String): List<AutofillId> =
    IntentCompat.getParcelableArrayListExtra(this, key, AutofillId::class.java).orEmpty()

package com.aozijx.passly.feature.autofill.presentation.legacy

import android.content.Intent
import android.view.autofill.AutofillId
import android.view.inputmethod.InlineSuggestionsRequest
import androidx.core.content.IntentCompat
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.platform.AutofillIntentExtras

internal fun Intent.toAutofillFillRequest(): AutofillFillRequest? {
    val interactionId =
        getStringExtra(AutofillIntentExtras.INTERACTION_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::AutofillInteractionId)
            ?: return null
    val packageName =
        getStringExtra(AutofillIntentExtras.PACKAGE_NAME)
            ?.takeIf(String::isNotBlank)
            ?: return null
    val uiMode =
        AutofillPresentation.fromWireKey(getStringExtra(AutofillIntentExtras.UI_MODE))
            ?: return null
    val resultTarget =
        LegacyAutofillResultTarget.fromWireValue(
            getStringExtra(AutofillIntentExtras.RESULT_TARGET),
        ) ?: return null

    return AutofillFillRequest(
        interactionId = interactionId,
        uiMode = uiMode,
        packageName = packageName,
        webDomain = getStringExtra(AutofillIntentExtras.WEB_DOMAIN),
        directEntryId =
            getStringExtra(AutofillIntentExtras.ENTRY_ID)
                ?.takeIf(String::isNotBlank)
                ?.let(::EntryId),
        resultTarget = resultTarget,
        inlineRequest =
            IntentCompat.getParcelableExtra(
                this,
                AutofillIntentExtras.INLINE_REQUEST,
                InlineSuggestionsRequest::class.java,
            ),
        editableIds =
            IntentCompat
                .getParcelableArrayExtra(
                    this,
                    AutofillIntentExtras.EDITABLE_IDS,
                    AutofillId::class.java,
                )?.filterIsInstance<AutofillId>()
                .orEmpty(),
        usernameIds = autofillIds(AutofillIntentExtras.USERNAME_IDS),
        passwordIds = autofillIds(AutofillIntentExtras.PASSWORD_IDS),
        otpIds = autofillIds(AutofillIntentExtras.OTP_IDS),
    )
}

private fun Intent.autofillIds(key: String): List<AutofillId> =
    IntentCompat.getParcelableArrayListExtra(this, key, AutofillId::class.java).orEmpty()

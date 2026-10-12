package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.autofill.AutofillId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutofillPendingIntentFactory @Inject constructor(
    private val launchTarget: AutofillLaunchTarget,
) {
    fun createCandidateIntent(
        context: Context,
        interactionId: AutofillInteractionId,
        entryId: EntryId,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>>,
        savePromptsEnabled: Boolean,
    ): Intent {
        val requestId = AutofillPendingIntentPolicy.legacyFillRequestId(
            interactionId.value,
            entryId.value,
        )
        return launchTarget.legacyFillIntent(context, requestId).apply {
            setIdentifier(requestId)
            putExtra(AutofillLaunchExtras.ENTRY_ID, entryId.value)
            putExtra(AutofillLaunchExtras.RETURN_DATASET, true)
            putCommonExtras(interactionId, parsed, uiMode, roleIds, savePromptsEnabled)
        }
    }

    fun createInteractionIntent(
        context: Context,
        interactionId: AutofillInteractionId,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>>,
        savePromptsEnabled: Boolean,
    ): Intent {
        val requestId = AutofillPendingIntentPolicy.legacyInteractionRequestId(interactionId.value)
        return launchTarget.legacyFillIntent(context, requestId).apply {
            setIdentifier(requestId)
            putCommonExtras(interactionId, parsed, uiMode, roleIds, savePromptsEnabled)
        }
    }

    private fun Intent.putCommonExtras(
        interactionId: AutofillInteractionId,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>>,
        savePromptsEnabled: Boolean,
    ) {
        putExtra(AutofillLaunchExtras.INTERACTION_ID, interactionId.value)
        putExtra(AutofillLaunchExtras.UI_MODE, uiMode.name)
        putExtra(AutofillLaunchExtras.PACKAGE_NAME, parsed.packageName)
        putExtra(AutofillLaunchExtras.WEB_DOMAIN, parsed.webDomain)
        putExtra(AutofillLaunchExtras.EDITABLE_IDS, parsed.allIds.toTypedArray())
        putExtra(AutofillLaunchExtras.SAVE_PROMPTS_ENABLED, savePromptsEnabled)
        roleIds[FieldRole.USERNAME]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { putExtra(AutofillLaunchExtras.USERNAME_IDS, ArrayList(it)) }
        roleIds[FieldRole.PASSWORD]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { putExtra(AutofillLaunchExtras.PASSWORD_IDS, ArrayList(it)) }
        roleIds[FieldRole.OTP]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { putExtra(AutofillLaunchExtras.OTP_IDS, ArrayList(it)) }
    }

    fun getActivityPendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
    )
}

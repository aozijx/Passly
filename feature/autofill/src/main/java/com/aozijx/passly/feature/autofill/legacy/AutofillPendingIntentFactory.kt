package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.autofill.AutofillId
import android.view.inputmethod.InlineSuggestionsRequest
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.platform.AutofillIntentExtras
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillPendingIntentFactory
    @Inject
    constructor(
        private val launchTarget: AutofillLaunchTarget,
    ) {
        fun createCandidateIntent(
            context: Context,
            interactionId: AutofillInteractionId,
            entryId: EntryId,
            packageName: String?,
            webDomain: String?,
            editableIds: List<AutofillId>,
            uiMode: AutofillPresentation,
            roleIds: Map<FieldRole, List<AutofillId>>,
            inlineRequest: InlineSuggestionsRequest? = null,
        ): Intent {
            val requestId =
                AutofillPendingIntentPolicy.legacyFillRequestId(
                    interactionId.value,
                    entryId.value,
                )
            return launchTarget.legacyFillIntent(context, requestId).apply {
                setIdentifier(requestId)
                putExtra(AutofillIntentExtras.ENTRY_ID, entryId.value)
                putCommonExtras(
                    interactionId,
                    packageName,
                    webDomain,
                    editableIds,
                    uiMode,
                    roleIds,
                    LegacyAutofillResultTarget.DATASET,
                    inlineRequest,
                )
            }
        }

        fun createPickerIntent(
            context: Context,
            interactionId: AutofillInteractionId,
            packageName: String?,
            webDomain: String?,
            editableIds: List<AutofillId>,
            uiMode: AutofillPresentation,
            roleIds: Map<FieldRole, List<AutofillId>>,
            inlineRequest: InlineSuggestionsRequest? = null,
        ): Intent {
            val requestId = AutofillPendingIntentPolicy.legacyInteractionRequestId(interactionId.value)
            return launchTarget.legacyFillIntent(context, requestId).apply {
                setIdentifier(requestId)
                putCommonExtras(
                    interactionId,
                    packageName,
                    webDomain,
                    editableIds,
                    uiMode,
                    roleIds,
                    LegacyAutofillResultTarget.DATASET,
                    inlineRequest,
                )
            }
        }

        fun createUnlockIntent(
            context: Context,
            interactionId: AutofillInteractionId,
            packageName: String?,
            webDomain: String?,
            editableIds: List<AutofillId>,
            uiMode: AutofillPresentation,
            roleIds: Map<FieldRole, List<AutofillId>>,
            resultTarget: LegacyAutofillResultTarget,
            inlineRequest: InlineSuggestionsRequest? = null,
        ): Intent {
            val requestId = AutofillPendingIntentPolicy.legacyInteractionRequestId(interactionId.value)
            return launchTarget.legacyFillIntent(context, requestId).apply {
                setIdentifier(requestId)
                putCommonExtras(
                    interactionId,
                    packageName,
                    webDomain,
                    editableIds,
                    uiMode,
                    roleIds,
                    resultTarget,
                    inlineRequest,
                )
            }
        }

        private fun Intent.putCommonExtras(
            interactionId: AutofillInteractionId,
            packageName: String?,
            webDomain: String?,
            editableIds: List<AutofillId>,
            uiMode: AutofillPresentation,
            roleIds: Map<FieldRole, List<AutofillId>>,
            resultTarget: LegacyAutofillResultTarget,
            inlineRequest: InlineSuggestionsRequest?,
        ) {
            putExtra(AutofillIntentExtras.INTERACTION_ID, interactionId.value)
            putExtra(AutofillIntentExtras.UI_MODE, uiMode.wireKey)
            inlineRequest?.let { putExtra(AutofillIntentExtras.INLINE_REQUEST, it) }
            putExtra(AutofillIntentExtras.RESULT_TARGET, resultTarget.wireValue)
            putExtra(AutofillIntentExtras.PACKAGE_NAME, packageName)
            putExtra(AutofillIntentExtras.WEB_DOMAIN, webDomain)
            putExtra(AutofillIntentExtras.EDITABLE_IDS, editableIds.toTypedArray())
            roleIds[FieldRole.USERNAME]
                ?.takeIf(List<AutofillId>::isNotEmpty)
                ?.let { putExtra(AutofillIntentExtras.USERNAME_IDS, ArrayList(it)) }
            roleIds[FieldRole.PASSWORD]
                ?.takeIf(List<AutofillId>::isNotEmpty)
                ?.let { putExtra(AutofillIntentExtras.PASSWORD_IDS, ArrayList(it)) }
            roleIds[FieldRole.OTP]
                ?.takeIf(List<AutofillId>::isNotEmpty)
                ?.let { putExtra(AutofillIntentExtras.OTP_IDS, ArrayList(it)) }
        }

        fun getActivityPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
            )
    }

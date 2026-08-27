package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.autofill.AutofillId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified PendingIntent Factory for Autofill.
 */
@Singleton
class AutofillPendingIntentFactory @Inject constructor(
    private val launchTarget: AutofillLaunchTarget,
) {

    fun createFillIntent(
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ): Intent {
        val requestId = AutofillPendingIntentPolicy.legacyFillRequestId(candidate.entry.id.value)
        return launchTarget.legacyFillIntent(context, requestId).apply {
            setIdentifier(requestId)
            putExtra("vault_item_id", candidate.entry.id.value)
            putExtra("autofill_ui_mode", uiMode.name)
            putExtra("package_name", parsed.packageName)
            putExtra("web_domain", parsed.webDomain)
            putExtra("editable_ids", parsed.allIds.toTypedArray())
            putRoleIds(this, roleIds)
            putExtra(AutofillLaunchExtras.RETURN_DATASET, true)
        }
    }

    fun createBaseIntent(
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ): Intent {
        val requestId = AutofillPendingIntentPolicy.legacyBaseRequestId(parsed.packageName)
        return launchTarget.legacyFillIntent(context, requestId).apply {
            setIdentifier(requestId)
            putExtra("autofill_ui_mode", uiMode.name)
            putExtra("package_name", parsed.packageName)
            putExtra("web_domain", parsed.webDomain)
            putExtra("editable_ids", parsed.allIds.toTypedArray())
            putRoleIds(this, roleIds)
        }
    }

    private fun putRoleIds(intent: Intent, roleIds: Map<FieldRole, List<AutofillId>>) {
        if (roleIds.isEmpty()) return
        roleIds[FieldRole.USERNAME]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { intent.putExtra("username_ids", ArrayList(it)) }
        roleIds[FieldRole.PASSWORD]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { intent.putExtra("password_ids", ArrayList(it)) }
        roleIds[FieldRole.OTP]?.takeIf(List<AutofillId>::isNotEmpty)
            ?.let { intent.putExtra("otp_ids", ArrayList(it)) }
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

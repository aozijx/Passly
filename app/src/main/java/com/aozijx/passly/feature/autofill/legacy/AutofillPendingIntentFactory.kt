package com.aozijx.passly.feature.autofill.legacy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.autofill.AutofillId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

/**
 * Unified PendingIntent Factory for Autofill.
 */
internal object AutofillPendingIntentFactory {

    fun createFillIntent(
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        setIdentifier("autofill-fill:${candidate.entry.id.value}")
        putExtra("vault_item_id", candidate.entry.id.value)
        putExtra("autofill_ui_mode", uiMode.name)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
        putExtra("editable_ids", parsed.allIds.toTypedArray())
        putRoleIds(this, roleIds)
        putExtra(AutofillFillActivity.EXTRA_RETURN_DATASET, true)
    }

    fun createBaseIntent(
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        setIdentifier("autofill-base:${parsed.packageName ?: "unknown"}")
        putExtra("autofill_ui_mode", uiMode.name)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
        putExtra("editable_ids", parsed.allIds.toTypedArray())
        putRoleIds(this, roleIds)
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
        authenticationPendingIntentFlags(),
    )

    private fun authenticationPendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_MUTABLE
}

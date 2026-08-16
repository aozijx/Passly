package com.aozijx.passly.feature.autofill.shared

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.framework.AutofillFillActivity
import com.aozijx.passly.feature.autofill.framework.service.parser.ParsedStructure

/**
 * 自动填充统一 PendingIntent 工厂。
 *
 * Legacy（AutofillService）与 Modern（CredentialProvider）两套平台都通过
 * PendingIntent 唤起二阶段交互 Activity，此工厂收敛构建参数与 flag，
 * 避免各模块各自拼装 intent extra / flag。
 */
internal object AutofillPendingIntentFactory {

    fun createFillIntent(
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("vault_item_id", candidate.candidateId)
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("autofill_ui_mode", uiMode.name)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
        // 样式化页面识别不出角色时，携带全部可编辑字段供填充阶段按顺序兜底。
        putExtra("editable_ids", parsed.allIds.toTypedArray())
        putExtra(AutofillFillActivity.EXTRA_RETURN_DATASET, true)
    }

    fun createBaseIntent(
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("autofill_ui_mode", uiMode.name)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
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

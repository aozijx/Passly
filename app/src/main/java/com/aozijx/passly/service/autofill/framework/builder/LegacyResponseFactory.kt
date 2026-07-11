package com.aozijx.passly.service.autofill.framework.builder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.service.autofill.framework.parser.ParsedStructure
import com.aozijx.passly.ui.features.autofill.framework.AutofillFillActivity
import com.aozijx.passly.ui.features.autofill.framework.AutofillRemoteViewFactory

/**
 * Legacy 自动填充响应工厂：负责将 InternalFillResponse 转换为 Android FillResponse。
 *
 * 包含两种路径：
 * - 未解锁：构建解锁触发器
 * - 已解锁：构建候选项 Dataset 列表
 * - 解锁后直填：构建直接填充的 Dataset 列表
 */
internal object LegacyResponseFactory {

    data class BasicCredentials(
        val username: String,
        val password: String
    )

    // ── Phase 1: 构建 FillResponse（未解锁/已解锁 Dataset 列表） ──

    fun buildFillResponse(
        context: Context,
        response: InternalFillResponse,
        parsed: ParsedStructure,
        uiMode: AutofillUiMode,
    ): FillResponse {
        val builder = FillResponse.Builder()

        if (response.candidates.isEmpty()) {
            val presentation = AutofillRemoteViewFactory.createUnlockTrigger(context)
            val intent = createUnlockIntent(context, parsed, uiMode)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            LegacyDatasetFactory.setAuthenticationCompat(
                builder,
                parsed.allIds.toTypedArray(),
                pi.intentSender,
                presentation
            )
        } else {
            response.candidates.forEach { candidate ->
                val presentation = AutofillRemoteViewFactory.createDatasetItem(
                    context = context,
                    candidate = candidate,
                    subtitle = candidate.subtitle,
                    badge = buildBadge(candidate),
                )
                val intent = createFillIntent(context, candidate, parsed, uiMode)
                val pi = PendingIntent.getActivity(
                    context, candidate.candidateId.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val dsBuilder = Dataset.Builder().setAuthentication(pi.intentSender)
                LegacyDatasetFactory.setMenuPresentationCompat(
                    dsBuilder,
                    parsed.allIds,
                    presentation
                )
                builder.addDataset(dsBuilder.build())
            }
        }
        return builder.build()
    }

    // ── Phase 2: 解锁后构建 FillResponse（多个候选项直接填充） ──

    fun buildPostUnlockFillResponse(
        context: Context,
        candidates: List<ResolvedCandidate>,
        usernameId: android.view.autofill.AutofillId?,
        passwordId: android.view.autofill.AutofillId?,
        otpId: android.view.autofill.AutofillId?,
    ): FillResponse? {
        val builder = FillResponse.Builder()
        var datasetCount = 0

        candidates.forEach { candidate ->
            val subtitle = buildSubtitle(candidate)
            val badge = buildBadge(candidate)
            val presentation = AutofillRemoteViewFactory.createDatasetItem(
                context = context, candidate = candidate, subtitle = subtitle, badge = badge
            )

            val basicCred = getBasicCredentials(candidate)
            if (basicCred != null) {
                val dataset = LegacyDatasetFactory.createFillDataset(
                    usernameId = usernameId,
                    passwordId = passwordId,
                    otpId = otpId,
                    username = basicCred.username,
                    password = basicCred.password,
                    totpCode = candidate.totpCode,
                    presentation = presentation
                )
                if (dataset != null) {
                    builder.addDataset(dataset)
                    datasetCount++
                }
            }
        }

        return if (datasetCount > 0) builder.build() else null
    }

    fun getBasicCredentials(candidate: ResolvedCandidate): BasicCredentials? {
        if (candidate.username.isBlank() && candidate.password.isBlank()) return null
        return BasicCredentials(candidate.username, candidate.password)
    }

    private fun buildSubtitle(candidate: ResolvedCandidate): String {
        val infoParts = mutableListOf<String>()
        if (candidate.username.isNotBlank()) infoParts += candidate.username
        val displayType = EntryType.fromValue(candidate.entryType).displayName
        if (infoParts.isEmpty()) infoParts += displayType
        val joined = infoParts.joinToString(" · ")
        return if (candidate.totpCode != null) "OTP · $joined" else joined
    }

    private fun buildBadge(candidate: ResolvedCandidate): String {
        return when (candidate.matchedBy) {
            MatchType.PACKAGE_NAME -> candidate.matchedPackage ?: ""
            MatchType.WEB_DOMAIN -> candidate.matchedDomain ?: ""
            else -> ""
        }
    }

    private fun createUnlockIntent(
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillUiMode
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("unlock_only", true)
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
        putExtra("autofill_ui_mode", uiMode.name)
    }

    private fun createFillIntent(
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillUiMode
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("vault_item_id", candidate.candidateId)
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("autofill_ui_mode", uiMode.name)
    }
}

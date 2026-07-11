package com.aozijx.passly.service.autofill.framework.builder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.model.toVaultEntry
import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
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
            response.candidates.forEach { entry ->
                val presentation = AutofillRemoteViewFactory.createDatasetItem(
                    context = context,
                    entry = entry.toVaultEntry(),
                    subtitle = entry.subtitle,
                    badge = "",
                )
                val intent = createFillIntent(context, entry, parsed, uiMode)
                val pi = PendingIntent.getActivity(
                    context, entry.candidateId.hashCode(), intent,
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
        candidates: List<CredentialCandidate>,
        usernameId: android.view.autofill.AutofillId?,
        passwordId: android.view.autofill.AutofillId?,
        otpId: android.view.autofill.AutofillId?,
    ): FillResponse? {
        val builder = FillResponse.Builder()
        var datasetCount = 0

        candidates.forEach { candidate ->
            val entry = candidate.entry
            val subtitle = buildSubtitle(entry)
            val badge = buildBadge(candidate)
            val presentation = AutofillRemoteViewFactory.createDatasetItem(
                context = context, entry = entry, subtitle = subtitle, badge = badge
            )

            val basicCred = getBasicCredentials(entry)
            if (basicCred != null) {
                val totpCode = if (otpId != null && entry.totpSecret?.isNotBlank() == true) {
                    TwoFAUtils.generateCurrentTotpFromEntry(entry)
                } else null

                val dataset = LegacyDatasetFactory.createFillDataset(
                    usernameId = usernameId,
                    passwordId = passwordId,
                    otpId = otpId,
                    username = basicCred.username,
                    password = basicCred.password,
                    totpCode = totpCode,
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

    // ── 辅助方法 ──

    fun getBasicCredentials(entry: VaultEntry): BasicCredentials? {
        val username = entry.username
        val password = entry.password
        if (username.isBlank() && password.isBlank()) return null
        return BasicCredentials(username, password)
    }

    private fun buildSubtitle(entry: VaultEntry): String {
        val strategy = runCatching {
            EntryTypeStrategyFactory.getStrategy(EntryType.fromValue(entry.entryType))
        }.getOrNull()

        val strategySummary = strategy
            ?.let { runCatching { it.extractSummary(entry) }.getOrDefault("") }
            .orEmpty()

        val infoParts = mutableListOf<String>()
        if (entry.username.isNotBlank()) infoParts += entry.username
        if (strategySummary.isNotBlank()) infoParts += strategySummary
        if (infoParts.isEmpty()) infoParts += EntryType.fromValue(entry.entryType).displayName
        val joined = infoParts.joinToString(" · ")
        return if (!entry.totpSecret.isNullOrBlank()) "OTP · $joined" else joined
    }

    private fun buildBadge(candidate: CredentialCandidate): String {
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
        entry: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillUiMode
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("vault_item_id", entry.candidateId)
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("autofill_ui_mode", uiMode.name)
    }
}
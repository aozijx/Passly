package com.aozijx.passly.service.autofill.framework.builder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import com.aozijx.passly.R
import com.aozijx.passly.core.autofill.model.FillAvailability
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.entry.model.lookup.MatchType
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.framework.AutofillFillActivity
import com.aozijx.passly.feature.autofill.framework.AutofillRemoteViewFactory
import com.aozijx.passly.service.autofill.framework.parser.ParsedStructure

/**
 * Legacy 自动填充响应工厂：负责将 InternalFillResponse 转换为 Android FillResponse。
 *
 * 包含两种路径：
 * - 未解锁：构建解锁触发器
 * - 已解锁：构建候选项认证入口列表
 * - 选中候选后：由 AutofillFillActivity 二阶段读取并构建单条直填 Dataset
 */
internal object LegacyResponseFactory {

    data class BasicCredentials(
        val username: String,
        val password: String
    )

    // ── Phase 1: 构建 FillResponse（未解锁/已解锁候选入口） ──

    fun buildFillResponse(
        context: Context,
        response: InternalFillResponse,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ): FillResponse {
        val builder = FillResponse.Builder()

        when {
            response.availability == FillAvailability.LOCKED -> {
                addUnlockAuthentication(builder, context, parsed, uiMode)
            }

            response.candidates.isNotEmpty() &&
                    uiMode == AutofillPresentation.BOTTOM_SHEET -> {
                addBottomSheetAuthentication(
                    builder,
                    context,
                    response.candidates,
                    parsed,
                    uiMode,
                )
            }

            response.candidates.isNotEmpty() -> response.candidates.forEach { candidate ->
                addCandidateAuthenticationDataset(builder, context, candidate, parsed, uiMode)
            }

            !response.savePromptsEnabled -> builder.disableAutofill(5_000)
        }

        if (response.savePromptsEnabled) addSaveInfo(builder, parsed)
        return builder.build()
    }

    fun buildCandidateAuthenticationResponse(
        context: Context,
        candidates: List<ResolvedCandidate>,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        otpId: AutofillId?,
        packageName: String?,
        webDomain: String?,
        uiMode: AutofillPresentation,
    ): FillResponse? {
        val parsed = ParsedStructure(
            usernameId = usernameId,
            passwordId = passwordId,
            otpId = otpId,
            packageName = packageName,
            webDomain = webDomain,
        )
        if (parsed.allIds.isEmpty() || candidates.isEmpty()) return null

        val builder = FillResponse.Builder()
        candidates.forEach { candidate ->
            addCandidateAuthenticationDataset(builder, context, candidate, parsed, uiMode)
        }
        return builder.build()
    }

    fun getBasicCredentials(candidate: ResolvedCandidate): BasicCredentials? {
        if (candidate.username.isBlank() && candidate.password.isBlank()) return null
        return BasicCredentials(candidate.username, candidate.password)
    }

    private fun addCandidateAuthenticationDataset(
        builder: FillResponse.Builder,
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ) {
        val presentation = AutofillRemoteViewFactory.createDatasetItem(
            context = context,
            candidate = candidate,
            badge = buildBadge(context, candidate),
        )
        val intent = createFillIntent(context, candidate, parsed, uiMode)
        val pi = PendingIntent.getActivity(
            context,
            candidate.candidateId.hashCode(),
            intent,
            authenticationPendingIntentFlags(),
        )
        val dsBuilder = Dataset.Builder().setAuthentication(pi.intentSender)
        LegacyDatasetFactory.setMenuPresentationCompat(
            dsBuilder,
            parsed.allIds,
            presentation,
        )
        builder.addDataset(dsBuilder.build())
    }

    private fun buildBadge(context: Context, candidate: ResolvedCandidate): String {
        return when (candidate.matchedBy) {
            MatchType.PACKAGE_NAME -> context.getString(R.string.autofill_badge_app)
            MatchType.WEB_DOMAIN -> context.getString(R.string.autofill_badge_website)
            else -> ""
        }
    }

    private fun createFillIntent(
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation
    ): Intent = Intent(context, AutofillFillActivity::class.java).apply {
        putExtra("vault_item_id", candidate.candidateId)
        putExtra("username_id", parsed.usernameId)
        putExtra("password_id", parsed.passwordId)
        putExtra("otp_id", parsed.otpId)
        putExtra("autofill_ui_mode", uiMode.name)
        putExtra("package_name", parsed.packageName)
        putExtra("web_domain", parsed.webDomain)
        putExtra(AutofillFillActivity.EXTRA_RETURN_DATASET, true)
    }

    private fun addUnlockAuthentication(
        builder: FillResponse.Builder,
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ) {
        if (parsed.allIds.isEmpty()) return
        val intent = createBaseIntent(context, parsed, uiMode).apply {
            putExtra("unlock_only", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            parsed.packageName.hashCode(),
            intent,
            authenticationPendingIntentFlags(),
        )
        LegacyDatasetFactory.setAuthenticationCompat(
            builder,
            parsed.allIds.toTypedArray(),
            pendingIntent.intentSender,
            AutofillRemoteViewFactory.createUnlockTrigger(context),
        )
    }

    private fun addBottomSheetAuthentication(
        builder: FillResponse.Builder,
        context: Context,
        candidates: List<ResolvedCandidate>,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ) {
        if (parsed.allIds.isEmpty()) return
        val intent = createBaseIntent(context, parsed, uiMode).apply {
            putExtra("vault_item_ids", candidates.map { it.candidateId }.toTypedArray())
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            candidates.map { it.candidateId }.hashCode(),
            intent,
            authenticationPendingIntentFlags(),
        )
        LegacyDatasetFactory.setAuthenticationCompat(
            builder,
            parsed.allIds.toTypedArray(),
            pendingIntent.intentSender,
            AutofillRemoteViewFactory.createBottomSheetTrigger(context, candidates.size),
        )
    }

    private fun createBaseIntent(
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

    private fun addSaveInfo(
        builder: FillResponse.Builder,
        parsed: ParsedStructure,
    ) {
        val requiredIds = listOfNotNull(parsed.usernameId, parsed.passwordId)
        if (requiredIds.isEmpty()) return
        var dataType = 0
        if (parsed.usernameId != null) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_USERNAME
        if (parsed.passwordId != null) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        builder.setSaveInfo(SaveInfo.Builder(dataType, requiredIds.toTypedArray()).build())
    }

    private fun authenticationPendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_MUTABLE
}

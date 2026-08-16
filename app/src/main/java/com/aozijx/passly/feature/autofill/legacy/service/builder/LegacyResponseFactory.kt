package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import com.aozijx.passly.R
import com.aozijx.passly.core.autofill.model.FillAvailability
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

/**
 * Legacy 自动填充响应工厂：负责将 InternalFillResponse 转换为 Android FillResponse。
 *
 * 包含两种路径：
 * - 未解锁：构建解锁触发器
 * - 已解锁：构建候选项认证入口列表
 * - 选中候选后：由 AutofillFillActivity 二阶段读取并构建单条直填 Dataset
 *
 * PendingIntent 构建收敛到 [AutofillPendingIntentFactory]，本类只组装响应结构。
 */
internal object LegacyResponseFactory {

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
        val intent = AutofillPendingIntentFactory.createFillIntent(
            context, candidate, parsed, uiMode
        )
        val pi = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            candidate.candidateId.hashCode(),
            intent,
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
            MatchType.APPLICATION_ID -> context.getString(R.string.autofill_badge_app)
            MatchType.WEB_DOMAIN -> context.getString(R.string.autofill_badge_website)
            else -> ""
        }
    }

    private fun addUnlockAuthentication(
        builder: FillResponse.Builder,
        context: Context,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ) {
        if (parsed.allIds.isEmpty()) return
        val intent = AutofillPendingIntentFactory.createBaseIntent(context, parsed, uiMode).apply {
            putExtra("unlock_only", true)
        }
        val pendingIntent = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            parsed.packageName.hashCode(),
            intent,
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
        val intent = AutofillPendingIntentFactory.createBaseIntent(context, parsed, uiMode).apply {
            putExtra("vault_item_ids", candidates.map { it.candidateId }.toTypedArray())
        }
        val pendingIntent = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            candidates.map { it.candidateId }.hashCode(),
            intent,
        )
        LegacyDatasetFactory.setAuthenticationCompat(
            builder,
            parsed.allIds.toTypedArray(),
            pendingIntent.intentSender,
            AutofillRemoteViewFactory.createBottomSheetTrigger(context, candidates.size),
        )
    }

    private fun addSaveInfo(
        builder: FillResponse.Builder,
        parsed: ParsedStructure,
    ) {
        // 优先用识别的角色字段；样式化页面识别不出时回退到全部可编辑字段，
        // 保证保存提示在自定义控件上也能触发。
        val requiredIds = listOfNotNull(parsed.usernameId, parsed.passwordId)
            .ifEmpty { parsed.allIds }
        if (requiredIds.isEmpty()) return
        var dataType = 0
        if (parsed.usernameId != null) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_USERNAME
        if (parsed.passwordId != null) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        if (dataType == 0) dataType = SaveInfo.SAVE_DATA_TYPE_PASSWORD
        builder.setSaveInfo(SaveInfo.Builder(dataType, requiredIds.toTypedArray()).build())
    }
}

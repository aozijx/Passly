package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import com.aozijx.passly.R
import com.aozijx.passly.domain.autofill.model.AutofillResponse
import com.aozijx.passly.domain.autofill.model.AutofillStatus
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.EditableFieldInfo
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

/**
 * Legacy Response Factory for converting domain AutofillResponse to Android FillResponse.
 */
internal object LegacyResponseFactory {

    fun buildFillResponse(
        context: Context,
        response: AutofillResponse,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
    ): FillResponse {
        val builder = FillResponse.Builder()
        val roleIds = resolveRoleIds(response.roleMap, parsed)

        when {
            response.status == AutofillStatus.LOCKED -> {
                addUnlockAuthentication(builder, context, parsed, uiMode, roleIds)
            }

            response.candidates.isNotEmpty() &&
                    uiMode == AutofillPresentation.BOTTOM_SHEET -> {
                addBottomSheetAuthentication(
                    builder,
                    context,
                    response.candidates,
                    parsed,
                    uiMode,
                    roleIds,
                )
            }

            response.candidates.isNotEmpty() -> response.candidates.forEach { candidate ->
                // If secrets are already available (unlocked + granted), use direct fill path.
                if (candidate.password.isNotBlank()) {
                    addDirectFillDataset(builder, context, candidate, parsed, roleIds)
                } else {
                    addCandidateAuthenticationDataset(
                        builder, context, candidate, parsed, uiMode, roleIds,
                    )
                }
            }

            !response.savePromptsEnabled -> builder.disableAutofill(5_000)
        }

        if (response.savePromptsEnabled) {
            addSaveInfo(builder, parsed, roleIds)
        }
        return builder.build()
    }

    private fun resolveRoleIds(
        roleMap: Map<String, FieldRole>,
        parsed: ParsedStructure,
    ): Map<FieldRole, List<AutofillId>> {
        if (roleMap.isEmpty() || parsed.editableFields.isEmpty()) return emptyMap()
        val idByString = parsed.editableFields.associate { it.autofillId.toString() to it.autofillId }
        val result = mutableMapOf<FieldRole, MutableList<AutofillId>>()
        roleMap.forEach { (viewId, role) ->
            val autofillId = idByString[viewId] ?: return@forEach
            result.getOrPut(role) { mutableListOf() }.add(autofillId)
        }
        return result
    }

    fun buildCandidateAuthenticationResponse(
        context: Context,
        candidates: List<ResolvedCandidate>,
        editableIds: List<AutofillId>,
        roleIds: Map<FieldRole, List<AutofillId>>,
        packageName: String?,
        webDomain: String?,
        uiMode: AutofillPresentation,
        savePromptsEnabled: Boolean,
    ): FillResponse? {
        val parsed = ParsedStructure(
            packageName = packageName,
            webDomain = webDomain,
            editableFields = editableIds.map { EditableFieldInfo(autofillId = it) }
        )
        if (parsed.allIds.isEmpty()) return null

        if (candidates.isEmpty() && !savePromptsEnabled) return null
        val builder = FillResponse.Builder()

        if (candidates.isNotEmpty()) {
            candidates.forEach { candidate ->
                if (candidate.password.isNotBlank()) {
                    addDirectFillDataset(builder, context, candidate, parsed, roleIds)
                } else {
                    addCandidateAuthenticationDataset(builder, context, candidate, parsed, uiMode, roleIds)
                }
            }
        }

        if (savePromptsEnabled) addSaveInfo(builder, parsed, roleIds)

        return builder.build()
    }

    private fun addDirectFillDataset(
        builder: FillResponse.Builder,
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        roleIds: Map<FieldRole, List<AutofillId>>
    ) {
        val presentation = AutofillRemoteViewFactory.createDatasetItem(
            context = context,
            candidate = candidate,
            badge = buildBadge(context, candidate),
        )

        val usernameIds = roleIds[FieldRole.USERNAME].orEmpty()
        val passwordIds = roleIds[FieldRole.PASSWORD].orEmpty()
        val otpIds = roleIds[FieldRole.OTP].orEmpty()

        val dsBuilder = Dataset.Builder()

        // First, apply presentation to ALL editable fields so Passly shows up everywhere
        LegacyDatasetFactory.setMenuPresentationCompat(
            dsBuilder,
            parsed.allIds,
            presentation
        )

        // Then, set the actual values for identified fields
        val ds = LegacyDatasetFactory.createFillDatasetForRoles(
            usernameIds = usernameIds,
            passwordIds = passwordIds,
            otpIds = otpIds,
            username = candidate.entry.username,
            password = candidate.password,
            totpCode = candidate.entry.otpPreview,
            presentation = presentation,
            existingBuilder = dsBuilder
        )

        if (ds != null) builder.addDataset(ds)
    }

    private fun addCandidateAuthenticationDataset(
        builder: FillResponse.Builder,
        context: Context,
        candidate: ResolvedCandidate,
        parsed: ParsedStructure,
        uiMode: AutofillPresentation,
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ) {
        val presentation = AutofillRemoteViewFactory.createDatasetItem(
            context = context,
            candidate = candidate,
            badge = buildBadge(context, candidate),
        )
        val intent = AutofillPendingIntentFactory.createFillIntent(
            context, candidate, parsed, uiMode, roleIds
        )
        val pi = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            candidate.entry.id.value.hashCode(),
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
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ) {
        if (parsed.allIds.isEmpty()) return
        val intent = AutofillPendingIntentFactory.createBaseIntent(context, parsed, uiMode, roleIds).apply {
            putExtra("unlock_only", true)
        }
        val pendingIntent = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            (parsed.packageName ?: "").hashCode(),
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
        roleIds: Map<FieldRole, List<AutofillId>> = emptyMap(),
    ) {
        if (parsed.allIds.isEmpty()) return
        val intent = AutofillPendingIntentFactory.createBaseIntent(context, parsed, uiMode, roleIds).apply {
            putExtra("vault_item_ids", candidates.map { it.entry.id.value }.toTypedArray())
        }
        val pendingIntent = AutofillPendingIntentFactory.getActivityPendingIntent(
            context,
            candidates.map { it.entry.id.value }.hashCode(),
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
        roleIds: Map<FieldRole, List<AutofillId>>
    ) {
        val usernameIds = roleIds[FieldRole.USERNAME].orEmpty()
        val passwordIds = roleIds[FieldRole.PASSWORD].orEmpty()

        val requiredIds = (usernameIds + passwordIds).ifEmpty { parsed.allIds }
        if (requiredIds.isEmpty()) return

        var dataType = 0
        if (usernameIds.isNotEmpty()) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_USERNAME
        if (passwordIds.isNotEmpty()) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        if (dataType == 0) dataType = SaveInfo.SAVE_DATA_TYPE_PASSWORD

        val saveInfoBuilder = SaveInfo.Builder(dataType, requiredIds.toTypedArray())
            .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)

        builder.setSaveInfo(saveInfoBuilder.build())
    }
}

package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import com.aozijx.passly.R
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyResponseFactory @Inject constructor(
    private val pendingIntentFactory: AutofillPendingIntentFactory,
) {
    fun buildFillResponse(
        context: Context,
        outcome: AutofillDiscoveryOutcome,
        parsed: ParsedStructure,
    ): FillResponse? = when (outcome) {
        AutofillDiscoveryOutcome.Disabled,
        AutofillDiscoveryOutcome.UnsupportedFields,
        is AutofillDiscoveryOutcome.Failed,
        -> null

        is AutofillDiscoveryOutcome.RequiresUnlock -> buildUnlockResponse(
            context,
            outcome.snapshot.id,
            outcome.context,
            parsed,
        )

        is AutofillDiscoveryOutcome.NoCandidates -> buildCandidateResponse(
            context = context,
            interactionId = outcome.snapshot.id,
            candidates = emptyList(),
            discoveryContext = outcome.context,
            parsed = parsed,
        )

        is AutofillDiscoveryOutcome.Candidates -> buildCandidateResponse(
            context = context,
            interactionId = outcome.snapshot.id,
            candidates = outcome.candidates,
            discoveryContext = outcome.context,
            parsed = parsed,
        )
    }

    fun buildDataset(
        payload: AutofillCredentialPayload,
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
        otpIds: List<AutofillId>,
    ): Dataset? = LegacyDatasetFactory.createFillDatasetForRoles(
        usernameIds = usernameIds,
        passwordIds = passwordIds,
        otpIds = otpIds,
        username = payload.username,
        password = payload.password,
        totpCode = payload.otpCode,
    )

    private fun buildUnlockResponse(
        context: Context,
        interactionId: AutofillInteractionId,
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
    ): FillResponse? {
        if (parsed.allIds.isEmpty()) return null
        val builder = FillResponse.Builder()
        val roleIds = resolveRoleIds(discoveryContext.roleMap, parsed)
        val intent = pendingIntentFactory.createInteractionIntent(
            context = context,
            interactionId = interactionId,
            parsed = parsed,
            uiMode = discoveryContext.presentation,
            roleIds = roleIds,
            savePromptsEnabled = discoveryContext.savePromptsEnabled,
        )
        val pendingIntent = pendingIntentFactory.getActivityPendingIntent(
            context,
            interactionId.value.hashCode(),
            intent,
        )
        LegacyDatasetFactory.setAuthenticationCompat(
            builder,
            parsed.allIds.toTypedArray(),
            pendingIntent.intentSender,
            AutofillRemoteViewFactory.createUnlockTrigger(context),
        )
        return builder.build()
    }

    private fun buildCandidateResponse(
        context: Context,
        interactionId: AutofillInteractionId,
        candidates: List<AutofillCandidateSummary>,
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
    ): FillResponse? {
        if (parsed.allIds.isEmpty()) return null
        if (candidates.isEmpty() && !discoveryContext.savePromptsEnabled) return null

        val builder = FillResponse.Builder()
        val roleIds = resolveRoleIds(discoveryContext.roleMap, parsed)
        if (candidates.isNotEmpty() &&
            discoveryContext.presentation == AutofillPresentation.BOTTOM_SHEET
        ) {
            addPickerAuthentication(
                builder,
                context,
                interactionId,
                candidates.size,
                discoveryContext,
                parsed,
                roleIds,
            )
        } else {
            candidates.forEach { candidate ->
                addCandidateAuthentication(
                    builder,
                    context,
                    interactionId,
                    candidate,
                    discoveryContext,
                    parsed,
                    roleIds,
                )
            }
        }
        if (discoveryContext.savePromptsEnabled) addSaveInfo(builder, parsed, roleIds)
        return builder.build()
    }

    private fun addCandidateAuthentication(
        builder: FillResponse.Builder,
        context: Context,
        interactionId: AutofillInteractionId,
        candidate: AutofillCandidateSummary,
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
        roleIds: Map<FieldRole, List<AutofillId>>,
    ) {
        val presentation = AutofillRemoteViewFactory.createDatasetItem(
            context = context,
            candidate = candidate,
            badge = buildBadge(context, candidate),
        )
        val intent = pendingIntentFactory.createCandidateIntent(
            context = context,
            interactionId = interactionId,
            entryId = candidate.entry.id,
            parsed = parsed,
            uiMode = discoveryContext.presentation,
            roleIds = roleIds,
            savePromptsEnabled = discoveryContext.savePromptsEnabled,
        )
        val requestCode = "${interactionId.value}:${candidate.entry.id.value}".hashCode()
        val pendingIntent = pendingIntentFactory.getActivityPendingIntent(context, requestCode, intent)
        val dataset = Dataset.Builder().setAuthentication(pendingIntent.intentSender)
        LegacyDatasetFactory.setMenuPresentationCompat(dataset, parsed.allIds, presentation)
        builder.addDataset(dataset.build())
    }

    private fun addPickerAuthentication(
        builder: FillResponse.Builder,
        context: Context,
        interactionId: AutofillInteractionId,
        candidateCount: Int,
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
        roleIds: Map<FieldRole, List<AutofillId>>,
    ) {
        val intent = pendingIntentFactory.createInteractionIntent(
            context = context,
            interactionId = interactionId,
            parsed = parsed,
            uiMode = discoveryContext.presentation,
            roleIds = roleIds,
            savePromptsEnabled = discoveryContext.savePromptsEnabled,
        )
        val pendingIntent = pendingIntentFactory.getActivityPendingIntent(
            context,
            interactionId.value.hashCode(),
            intent,
        )
        LegacyDatasetFactory.setAuthenticationCompat(
            builder,
            parsed.allIds.toTypedArray(),
            pendingIntent.intentSender,
            AutofillRemoteViewFactory.createBottomSheetTrigger(context, candidateCount),
        )
    }

    private fun resolveRoleIds(
        roleMap: Map<String, FieldRole>,
        parsed: ParsedStructure,
    ): Map<FieldRole, List<AutofillId>> {
        val idByString = parsed.editableFields.associate { it.autofillId.toString() to it.autofillId }
        return roleMap.entries.groupBy(
            keySelector = Map.Entry<String, FieldRole>::value,
            valueTransform = { idByString[it.key] },
        ).mapValues { (_, ids) -> ids.filterNotNull() }
    }

    private fun buildBadge(context: Context, candidate: AutofillCandidateSummary): String =
        when (candidate.match.type) {
            MatchType.APPLICATION_ID -> context.getString(R.string.autofill_badge_app)
            MatchType.WEB_DOMAIN -> context.getString(R.string.autofill_badge_website)
            else -> ""
        }

    private fun addSaveInfo(
        builder: FillResponse.Builder,
        parsed: ParsedStructure,
        roleIds: Map<FieldRole, List<AutofillId>>,
    ) {
        val usernameIds = roleIds[FieldRole.USERNAME].orEmpty()
        val passwordIds = roleIds[FieldRole.PASSWORD].orEmpty()
        val requiredIds = (usernameIds + passwordIds).ifEmpty { parsed.allIds }
        if (requiredIds.isEmpty()) return

        var dataType = 0
        if (usernameIds.isNotEmpty()) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_USERNAME
        if (passwordIds.isNotEmpty()) dataType = dataType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        if (dataType == 0) dataType = SaveInfo.SAVE_DATA_TYPE_PASSWORD
        builder.setSaveInfo(
            SaveInfo.Builder(dataType, requiredIds.toTypedArray())
                .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                .build(),
        )
    }
}

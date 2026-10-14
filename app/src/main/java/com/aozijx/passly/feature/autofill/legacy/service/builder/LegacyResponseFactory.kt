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
        addSaveInfo(builder, discoveryContext, parsed)
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
        val hasSaveInfo = addSaveInfo(builder, discoveryContext, parsed)
        if (candidates.isEmpty() && !hasSaveInfo) return null
        return builder.build()
    }

    private fun addSaveInfo(
        builder: FillResponse.Builder,
        discoveryContext: AutofillDiscoveryContext,
        parsed: ParsedStructure,
    ): Boolean {
        if (!discoveryContext.savePromptsEnabled) return false
        val plan = LegacySaveInfoPolicy.plan(discoveryContext.roleMap) ?: return false
        val idByString = parsed.editableFields.associate { it.autofillId.toString() to it.autofillId }
        val requiredPasswordIds = plan.requiredPasswordFieldIds.mapNotNull(idByString::get)
        if (requiredPasswordIds.isEmpty()) return false
        val optionalUsernameIds = plan.optionalUsernameFieldIds.mapNotNull(idByString::get)
        val dataTypes = SaveInfo.SAVE_DATA_TYPE_PASSWORD or
            if (optionalUsernameIds.isNotEmpty()) SaveInfo.SAVE_DATA_TYPE_USERNAME else 0
        val saveInfo = SaveInfo.Builder(dataTypes, requiredPasswordIds.toTypedArray()).apply {
            if (optionalUsernameIds.isNotEmpty()) {
                setOptionalIds(optionalUsernameIds.toTypedArray())
            }
            setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }.build()
        builder.setSaveInfo(saveInfo)
        return true
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

}

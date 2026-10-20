package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.aozijx.passly.R
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.FieldRole
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
    ): FillResponse? = buildResult(context, outcome, parsed).response

    fun buildResult(
        context: Context,
        outcome: AutofillDiscoveryOutcome,
        parsed: ParsedStructure,
    ): LegacyResponseBuildResult = when (outcome) {
        AutofillDiscoveryOutcome.Disabled,
        -> unavailable(Reasons.SessionRestricted)

        AutofillDiscoveryOutcome.UnsupportedFields -> unavailable(Reasons.NoFillableFields)

        is AutofillDiscoveryOutcome.Failed -> unavailable(Reasons.DatabaseUnavailable)

        is AutofillDiscoveryOutcome.RequiresUnlock -> completed(
            interactionId = outcome.snapshot.id,
            response = buildUnlockResponse(
                context,
                outcome.snapshot.id,
                outcome.context,
                parsed,
            ),
        )

        is AutofillDiscoveryOutcome.NoCandidates -> LegacyResponseBuildResult(
            response = buildCandidateResponse(
                context = context,
                interactionId = outcome.snapshot.id,
                candidates = emptyList(),
                discoveryContext = outcome.context,
                parsed = parsed,
            ),
            observation = LegacyDiscoveryObservation.Unavailable(Reasons.NoMatchingEntry),
        )

        is AutofillDiscoveryOutcome.Candidates -> completed(
            interactionId = outcome.snapshot.id,
            response = buildCandidateResponse(
                context = context,
                interactionId = outcome.snapshot.id,
                candidates = outcome.candidates,
                discoveryContext = outcome.context,
                parsed = parsed,
            ),
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
            AutofillRemoteViewFactory.render(
                context,
                AutofillRemoteItem.SingleLine(context.getString(R.string.vault_locked_title)),
            ),
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
        val saveInfo = LegacySaveInfoFactory.create(discoveryContext, parsed) ?: return false
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
        val subtitle = candidate.entry.username.trim()
        val item = if (subtitle.isEmpty()) {
            AutofillRemoteItem.SingleLine(candidate.entry.title)
        } else {
            AutofillRemoteItem.DoubleLine(candidate.entry.title, subtitle)
        }
        val presentation = AutofillRemoteViewFactory.render(context, item)
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
            AutofillRemoteViewFactory.render(
                context,
                AutofillRemoteItem.DoubleLine(
                    title = context.getString(
                        R.string.autofill_prompt_title,
                        context.getString(R.string.app_name),
                    ),
                    subtitle = context.resources.getQuantityString(
                        R.plurals.autofill_trigger_count,
                        candidateCount,
                        candidateCount,
                    ),
                ),
            ),
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

    private fun unavailable(reason: ReasonCode) = LegacyResponseBuildResult(
        response = null,
        observation = LegacyDiscoveryObservation.Unavailable(reason),
    )

    private fun completed(
        interactionId: AutofillInteractionId,
        response: FillResponse?,
    ) = LegacyResponseBuildResult(
        response = response,
        observation = LegacyDiscoveryObservation.Completed(interactionId),
    )
}

package com.aozijx.passly.feature.autofill.legacy.service.builder

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.inputmethod.InlineSuggestionsRequest
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.R
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LegacyResponseFactory
    @Inject
    internal constructor(
        private val pendingIntentFactory: AutofillPendingIntentFactory,
    ) {
        internal fun buildResult(
            context: Context,
            outcome: AutofillDiscoveryOutcome,
            parsed: ParsedStructure,
            inlineRequest: InlineSuggestionsRequest? = null,
        ): LegacyResponseBuildResult =
            when (outcome) {
                AutofillDiscoveryOutcome.Disabled,
                -> {
                    unavailable(Reasons.SessionRestricted)
                }

                AutofillDiscoveryOutcome.UnsupportedFields -> {
                    unavailable(Reasons.NoFillableFields)
                }

                is AutofillDiscoveryOutcome.Failed -> {
                    unavailable(Reasons.DatabaseUnavailable)
                }

                is AutofillDiscoveryOutcome.RequiresUnlock -> {
                    completed(
                        interactionId = outcome.snapshot.id,
                        built =
                            buildAuthenticationPromptResponse(
                                context,
                                outcome.snapshot.id,
                                outcome.context,
                                parsed,
                                inlineRequest,
                            ),
                    )
                }

                is AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation -> {
                    completed(
                        interactionId = outcome.snapshot.id,
                        built =
                            buildAuthenticationPromptResponse(
                                context,
                                outcome.snapshot.id,
                                outcome.context,
                                parsed,
                                inlineRequest,
                                promptTitle = R.string.autofill_review_web_request,
                            ),
                    )
                }

                is AutofillDiscoveryOutcome.NoCandidates -> {
                    result(
                        built =
                            buildCandidateResponse(
                                context = context,
                                interactionId = outcome.snapshot.id,
                                candidates = emptyList(),
                                discoveryContext = outcome.context,
                                parsed = parsed,
                                inlineRequest = inlineRequest,
                            ),
                        observation = LegacyDiscoveryObservation.Unavailable(Reasons.NoMatchingEntry),
                    )
                }

                is AutofillDiscoveryOutcome.Candidates -> {
                    completed(
                        interactionId = outcome.snapshot.id,
                        built =
                            buildCandidateResponse(
                                context = context,
                                interactionId = outcome.snapshot.id,
                                candidates = outcome.candidates,
                                discoveryContext = outcome.context,
                                parsed = parsed,
                                inlineRequest = inlineRequest,
                            ),
                    )
                }

                is AutofillDiscoveryOutcome.ResumeSelection -> {
                    unavailable(Reasons.UnknownFailure)
                }
            }

        fun buildAuthenticationResponse(
            context: Context,
            outcome: AutofillDiscoveryOutcome,
            parsed: ParsedStructure,
            inlineRequest: InlineSuggestionsRequest? = null,
        ): FillResponse? = buildResult(context, outcome, parsed, inlineRequest).response

        fun buildDataset(
            payload: AutofillCredentialPayload,
            usernameIds: List<AutofillId>,
            passwordIds: List<AutofillId>,
            otpIds: List<AutofillId>,
        ): Dataset? =
            LegacyDatasetFactory.createFillDatasetForRoles(
                usernameIds = usernameIds,
                passwordIds = passwordIds,
                otpIds = otpIds,
                username = payload.username,
                password = payload.password,
                totpCode = payload.otpCode,
            )

        private fun buildAuthenticationPromptResponse(
            context: Context,
            interactionId: AutofillInteractionId,
            discoveryContext: AutofillDiscoveryContext,
            parsed: ParsedStructure,
            inlineRequest: InlineSuggestionsRequest?,
            promptTitle: Int = R.string.vault_locked_title,
        ): BuiltResponse {
            if (parsed.allIds.isEmpty()) return BuiltResponse(response = null)
            val builder = FillResponse.Builder()
            val roleIds = resolveRoleIds(discoveryContext.roleMap, parsed)
            val resultTarget = LegacyAutofillResultTarget.forLockedPresentation(discoveryContext.presentation)
            val intent =
                pendingIntentFactory.createUnlockIntent(
                    context = context,
                    interactionId = interactionId,
                    parsed = parsed,
                    uiMode = discoveryContext.presentation,
                    roleIds = roleIds,
                    resultTarget = resultTarget,
                    inlineRequest = inlineRequest,
                )
            val pendingIntent =
                pendingIntentFactory.getActivityPendingIntent(
                    context,
                    interactionId.value.hashCode(),
                    intent,
                )
            val lockedPresentation =
                AutofillRemoteViewFactory.render(
                    context,
                    AutofillRemoteItem.SingleLine(context.getString(promptTitle)),
                )
            when (resultTarget) {
                LegacyAutofillResultTarget.DATASET -> {
                    builder.addDataset(
                        LegacyDatasetFactory.createAuthenticatedPickerDataset(
                            ids = parsed.allIds,
                            authentication = pendingIntent.intentSender,
                            presentation = lockedPresentation,
                        ),
                    )
                }

                LegacyAutofillResultTarget.RESPONSE -> {
                    LegacyDatasetFactory.setAuthenticationCompat(
                        builder,
                        parsed.allIds.toTypedArray(),
                        pendingIntent.intentSender,
                        lockedPresentation,
                        inlinePresentation =
                            if (discoveryContext.presentation == AutofillPresentation.SYSTEM_INLINE) {
                                LegacyInlinePresentationFactory.create(
                                    inlineRequest,
                                    0,
                                    context.getString(R.string.vault_locked_title),
                                    null,
                                    pendingIntent,
                                )
                            } else {
                                null
                            },
                    )
                }
            }
            val hasSaveInfo = addSaveInfo(builder, discoveryContext, parsed)
            val datasetCount = if (resultTarget == LegacyAutofillResultTarget.DATASET) 1 else 0
            return BuiltResponse(
                response = builder.build(),
                datasetCount = datasetCount,
                hasSaveInfo = hasSaveInfo,
            )
        }

        private fun buildCandidateResponse(
            context: Context,
            interactionId: AutofillInteractionId,
            candidates: List<AutofillCandidateSummary>,
            discoveryContext: AutofillDiscoveryContext,
            parsed: ParsedStructure,
            inlineRequest: InlineSuggestionsRequest?,
        ): BuiltResponse {
            if (parsed.allIds.isEmpty()) return BuiltResponse(response = null)

            val builder = FillResponse.Builder()
            val roleIds = resolveRoleIds(discoveryContext.roleMap, parsed)
            val pickerDataset =
                candidates.isNotEmpty() &&
                    discoveryContext.presentation == AutofillPresentation.BOTTOM_SHEET
            if (pickerDataset) {
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
                candidates.forEachIndexed { index, candidate ->
                    addCandidateAuthentication(
                        builder,
                        context,
                        interactionId,
                        candidate,
                        discoveryContext,
                        parsed,
                        roleIds,
                        inlineRequest =
                            if (discoveryContext.presentation == AutofillPresentation.SYSTEM_INLINE) {
                                inlineRequest
                            } else {
                                null
                            },
                        suggestionIndex = index,
                    )
                }
            }
            val hasSaveInfo = addSaveInfo(builder, discoveryContext, parsed)
            if (candidates.isEmpty() && !hasSaveInfo) return BuiltResponse(response = null)
            val datasetCount = if (pickerDataset) 1 else candidates.size
            return BuiltResponse(
                response = builder.build(),
                datasetCount = datasetCount,
                hasSaveInfo = hasSaveInfo,
            )
        }

        private fun addSaveInfo(
            builder: FillResponse.Builder,
            discoveryContext: AutofillDiscoveryContext,
            parsed: ParsedStructure,
        ): Boolean {
            val saveInfo = LegacySaveInfoFactory.create(discoveryContext.roleMap, parsed) ?: return false
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
            inlineRequest: InlineSuggestionsRequest?,
            suggestionIndex: Int,
        ) {
            val subtitle = candidate.entry.username.trim()
            val item =
                if (subtitle.isEmpty()) {
                    AutofillRemoteItem.SingleLine(candidate.entry.title)
                } else {
                    AutofillRemoteItem.DoubleLine(candidate.entry.title, subtitle)
                }
            val presentation = AutofillRemoteViewFactory.render(context, item)
            val intent =
                pendingIntentFactory.createCandidateIntent(
                    context = context,
                    interactionId = interactionId,
                    entryId = candidate.entry.id,
                    parsed = parsed,
                    uiMode = discoveryContext.presentation,
                    roleIds = roleIds,
                    inlineRequest = inlineRequest,
                )
            val requestCode = "${interactionId.value}:${candidate.entry.id.value}".hashCode()
            val pendingIntent = pendingIntentFactory.getActivityPendingIntent(context, requestCode, intent)
            val dataset = Dataset.Builder().setAuthentication(pendingIntent.intentSender)
            val inlinePresentation =
                LegacyInlinePresentationFactory.create(
                    inlineRequest,
                    suggestionIndex,
                    candidate.entry.title,
                    subtitle.takeIf(String::isNotBlank),
                    pendingIntent,
                )
            LegacyDatasetFactory.setMenuPresentationCompat(
                dataset,
                parsed.allIds,
                presentation,
                inlinePresentation,
            )
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
            val intent =
                pendingIntentFactory.createPickerIntent(
                    context = context,
                    interactionId = interactionId,
                    parsed = parsed,
                    uiMode = discoveryContext.presentation,
                    roleIds = roleIds,
                )
            val pendingIntent =
                pendingIntentFactory.getActivityPendingIntent(
                    context,
                    interactionId.value.hashCode(),
                    intent,
                )
            builder.addDataset(
                LegacyDatasetFactory.createAuthenticatedPickerDataset(
                    ids = parsed.allIds,
                    authentication = pendingIntent.intentSender,
                    presentation =
                        AutofillRemoteViewFactory.render(
                            context,
                            AutofillRemoteItem.DoubleLine(
                                title =
                                    context.getString(
                                        R.string.autofill_prompt_title,
                                        context.getString(R.string.app_name),
                                    ),
                                subtitle =
                                    context.resources.getQuantityString(
                                        R.plurals.autofill_trigger_count,
                                        candidateCount,
                                        candidateCount,
                                    ),
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
            return roleMap.entries
                .groupBy(
                    keySelector = Map.Entry<String, FieldRole>::value,
                    valueTransform = { idByString[it.key] },
                ).mapValues { (_, ids) -> ids.filterNotNull() }
        }

        private fun unavailable(reason: ReasonCode) =
            result(
                built = BuiltResponse(response = null),
                observation = LegacyDiscoveryObservation.Unavailable(reason),
            )

        private fun completed(
            interactionId: AutofillInteractionId,
            built: BuiltResponse,
        ) = result(built, LegacyDiscoveryObservation.Completed(interactionId))

        private fun result(
            built: BuiltResponse,
            observation: LegacyDiscoveryObservation,
        ) = LegacyResponseBuildResult(
            response = built.response,
            observation = observation,
            summary =
                LegacyResponseBuildResult.Summary(
                    datasetCount = built.datasetCount,
                    hasSaveInfo = built.hasSaveInfo,
                ),
        )

        private data class BuiltResponse(
            val response: FillResponse?,
            val datasetCount: Int = 0,
            val hasSaveInfo: Boolean = false,
        )
    }

package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.internal.di.Heuristic
import com.aozijx.passly.feature.autofill.internal.di.Strict
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Singleton
class DiscoverAutofillCandidates @Inject constructor(
    private val settingsSource: InteractionSettingsSource,
    private val candidateRepository: AutofillCandidateRepository,
    @param:Heuristic private val legacyFieldMatchStrategy: FieldMatchStrategy,
    @param:Strict private val credentialFieldMatchStrategy: FieldMatchStrategy,
    private val startInteraction: StartAutofillInteraction,
    private val interactionStore: AutofillInteractionStore,
    private val finishInteraction: FinishAutofillInteraction,
) {
    suspend operator fun invoke(
        request: AutofillRequest,
        interactionId: AutofillInteractionId? = null,
        classifiedRoles: Map<String, FieldRole>? = null,
    ): AutofillDiscoveryOutcome {
        val policy = settingsSource.interaction.first().autofill
        if (!policy.enabled || !policy.supports(request.source)) {
            return AutofillDiscoveryOutcome.Disabled
        }

        val match = classifiedRoles?.let { roles ->
            MatchResult(
                roleMap = roles,
                hasCredentials = roles.values.any {
                    it == FieldRole.USERNAME || it == FieldRole.PASSWORD || it == FieldRole.OTP
                },
            )
        } ?: request.source.fieldMatchStrategy().match(request)
        if (!match.hasCredentials) return AutofillDiscoveryOutcome.UnsupportedFields

        val scope = AutofillGrantContext(request.packageName, request.domain).normalized()
        val snapshot = resolveInteraction(request, scope, policy, interactionId)
            ?: return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INTERACTION_NOT_FOUND)
        if (snapshot.scope != scope) {
            return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.SCOPE_MISMATCH)
        }

        val context = AutofillDiscoveryContext(
            roleMap = match.roleMap,
            presentation = policy.presentation,
            requireAuthentication = policy.requireAuthentication,
            includeOtp = policy.includeOtp,
            savePromptsEnabled = policy.savePromptsEnabled,
        )
        if (snapshot.state == AutofillInteractionState.RequiresUnlock) {
            return AutofillDiscoveryOutcome.RequiresUnlock(snapshot, context)
        }
        if (snapshot.state != AutofillInteractionState.Authorized &&
            snapshot.state != AutofillInteractionState.CandidatesReady
        ) {
            return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_INTERACTION_STATE)
        }

        val candidates = try {
            candidateRepository.search(
                AutofillCandidateQuery(
                    packageName = scope.packageName,
                    webDomain = scope.webDomain,
                    allowUnmatched = policy.allowUnmatchedSuggestions,
                    limit = policy.normalizedMaxSuggestions,
                ),
            )
        } catch (cancelled: CancellationException) {
            finishInteraction.cancel(snapshot.id)
            throw cancelled
        } catch (_: Exception) {
            finishInteraction.cancel(snapshot.id)
            return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.CANDIDATE_QUERY_FAILED)
        }

        val recorded = interactionStore.recordCandidates(
            snapshot.id,
            candidates.mapTo(linkedSetOf()) { it.entry.id },
        )
        val recordedSnapshot = when (recorded) {
            is AutofillInteractionUpdate.Updated -> recorded.snapshot
            is AutofillInteractionUpdate.Rejected -> {
                return AutofillDiscoveryOutcome.Failed(
                    AutofillOperationFailure.INVALID_INTERACTION_STATE,
                )
            }
        }
        return if (candidates.isEmpty()) {
            AutofillDiscoveryOutcome.NoCandidates(recordedSnapshot, context)
        } else {
            AutofillDiscoveryOutcome.Candidates(recordedSnapshot, candidates, context)
        }
    }

    private suspend fun resolveInteraction(
        request: AutofillRequest,
        scope: AutofillGrantContext,
        policy: AutofillSettings,
        interactionId: AutofillInteractionId?,
    ): AutofillInteractionSnapshot? = if (interactionId == null) {
        startInteraction(
            source = request.source.toInteractionSource(),
            scope = scope,
            requireAuthentication = policy.requireAuthentication,
        )
    } else {
        interactionStore.snapshot(interactionId)
    }

    private fun AutofillSettings.supports(source: AutofillSource): Boolean = when (source) {
        AutofillSource.AUTOFILL_SERVICE -> true
        AutofillSource.CREDENTIAL_MANAGER -> credentialManagerEnabled
    }

    private fun AutofillSource.toInteractionSource(): AutofillInteractionSource = when (this) {
        AutofillSource.AUTOFILL_SERVICE -> AutofillInteractionSource.LEGACY
        AutofillSource.CREDENTIAL_MANAGER -> AutofillInteractionSource.CREDENTIAL_MANAGER
    }

    private fun AutofillSource.fieldMatchStrategy(): FieldMatchStrategy = when (this) {
        AutofillSource.AUTOFILL_SERVICE -> legacyFieldMatchStrategy
        AutofillSource.CREDENTIAL_MANAGER -> credentialFieldMatchStrategy
    }
}

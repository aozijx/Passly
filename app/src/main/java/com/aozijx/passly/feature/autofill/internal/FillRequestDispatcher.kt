package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillResponse
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.AutofillStatus
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Unified Filling Pipeline Orchestrator.
 */
class FillRequestDispatcher(
    private val sessionState: SecureSessionAccessState,
    private val candidateRetriever: CandidateRetriever,
    private val settingsRepository: AppSettingsRepository,
    private val grantStore: AutofillGrantStore,
    private val fieldMatchStrategy: FieldMatchStrategy,
) {

    suspend fun dispatch(request: AutofillRequest): AutofillResponse {
        val policySnapshot = settingsRepository.settings.first()
        val policy = policySnapshot.interaction.autofill
        if (!policy.enabled || !policy.supports(request.source)) {
            return AutofillResponse(status = AutofillStatus.DISABLED)
        }

        val matchResult = fieldMatchStrategy.match(request)

        // No credential fields identified → Don't trigger.
        if (!matchResult.hasCredentials) {
            return AutofillResponse(status = AutofillStatus.UNSUPPORTED_FIELDS)
        }

        if (!sessionState.hasFullSecureSessionAccess()) {
            return AutofillResponse(
                status = AutofillStatus.LOCKED,
                roleMap = matchResult.roleMap,
                requireAuthentication = policy.requireAuthentication,
                presentation = policy.presentation,
            )
        }

        val hasGrant = grantStore.isGranted(
            AutofillGrantContext(packageName = request.packageName, webDomain = request.domain)
        )
        val includeSecrets = hasGrant || !policy.requireAuthentication
        val candidates = candidateRetriever.resolve(request, policy, includeSecrets = includeSecrets)

        if (candidates.isEmpty()) {
            return AutofillResponse(
                status = AutofillStatus.NO_MATCH,
                roleMap = matchResult.roleMap,
                requireAuthentication = policy.requireAuthentication,
                savePromptsEnabled = policy.savePromptsEnabled,
                presentation = policy.presentation,
            )
        }

        return AutofillResponse(
            candidates = candidates,
            roleMap = matchResult.roleMap,
            requireAuthentication = policy.requireAuthentication,
            savePromptsEnabled = policy.savePromptsEnabled,
            presentation = policy.presentation,
        )
    }

    private fun AutofillSettings.supports(
        source: AutofillSource,
    ): Boolean = when (source) {
        AutofillSource.AUTOFILL_SERVICE -> true
        AutofillSource.CREDENTIAL_MANAGER -> credentialManagerEnabled
    }
}

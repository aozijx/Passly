package com.aozijx.passly.feature.autofill.framework

import android.content.Context
import android.service.autofill.FillResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.usecase.AutofillUseCases
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.autofill.AutofillRequestSession
import com.aozijx.passly.service.autofill.framework.builder.LegacyDatasetFactory
import com.aozijx.passly.service.autofill.framework.builder.LegacyResponseFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillFillViewModel @Inject constructor(
    private val autofillUseCases: AutofillUseCases,
    private val candidateResolver: CandidateResolver,
    private val vaultAccessState: SecureSessionAccessState,
    private val settingsRepository: AppSettingsRepository,
    private val requestSession: AutofillRequestSession,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutofillFillUiState>(AutofillFillUiState.Initial)
    val uiState: StateFlow<AutofillFillUiState> = _uiState.asStateFlow()

    private var currentRequest: AutofillFillRequest? = null
    private var authenticatedForCurrentRequest = false

    fun onIntent(intent: AutofillFillIntent) {
        when (intent) {
            is AutofillFillIntent.Initialize -> initialize(intent.request)
            is AutofillFillIntent.CandidateSelected -> selectCandidate(intent.candidate)
        }
    }

    private fun initialize(request: AutofillFillRequest) {
        currentRequest = request
        viewModelScope.launch {
            mutate(AutofillFillMutation.Loading)
            try {
                val settings = settingsRepository.settings.first().interaction.autofill
                if (request.isUnlockOnly) {
                    handleUnlockOnly(request, settings)
                    return@launch
                }
                if (!vaultAccessState.hasFullSecureSessionAccess()) {
                    val authentication = authenticateForAutofill()
                    if (authentication !is AuthenticationResult.Success) {
                        mutate(AutofillFillMutation.Completed(null))
                        return@launch
                    }
                    authenticatedForCurrentRequest = true
                }
                if (
                    request.uiMode == AutofillPresentation.BOTTOM_SHEET &&
                    request.candidateEntryIds.isNotEmpty() &&
                    request.directEntryId == null
                ) {
                    val candidates = candidateResolver.resolveByIds(
                        request.candidateEntryIds,
                        settings,
                    )
                    if (candidates.isEmpty()) {
                        mutate(AutofillFillMutation.Failed("No matching entries"))
                    } else {
                        mutate(AutofillFillMutation.CandidatesAvailable(candidates))
                    }
                } else {
                    if (!ensureAuthenticatedForSecretAccess(settings)) return@launch
                    val candidate = loadSelectedCandidate(
                        request.directEntryId,
                        request,
                        settings,
                    )
                    if (candidate == null) {
                        mutate(AutofillFillMutation.Failed("Entry not found"))
                    } else {
                        handleSingleEntry(candidate, request)
                    }
                }
            } catch (e: Exception) {
                AppTelemetry.e("AutofillVM", "Error", e)
                mutate(AutofillFillMutation.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun handleUnlockOnly(
        request: AutofillFillRequest,
        settings: AutofillSettings,
    ) {
        val authResult = authenticateForAutofill()
        if (authResult !is AuthenticationResult.Success) {
            mutate(AutofillFillMutation.Completed(null))
            return
        }
        authenticatedForCurrentRequest = true
        val candidates = candidateResolver.resolveByPackage(
            request.packageName,
            request.webDomain,
            settings,
            includeSecrets = false,
        )
        if (candidates.isEmpty()) {
            mutate(AutofillFillMutation.Completed(null))
            return
        }
        val response = LegacyResponseFactory.buildCandidateAuthenticationResponse(
            appContext,
            candidates = candidates,
            usernameId = request.usernameId,
            passwordId = request.passwordId,
            otpId = request.otpId,
            packageName = request.packageName,
            webDomain = request.webDomain,
            uiMode = request.uiMode,
        )
        mutate(
            AutofillFillMutation.Completed(
                response?.let(AutofillAuthenticationPayload::Response)
            )
        )
    }

    private suspend fun handleSingleEntry(
        candidate: ResolvedCandidate,
        request: AutofillFillRequest,
    ) {
        val basicCred = LegacyResponseFactory.getBasicCredentials(candidate)
        if (basicCred == null) {
            mutate(AutofillFillMutation.Failed("Failed to decrypt credentials"))
            return
        }

        val totpCode = if (request.otpId != null) candidate.totpCode else null

        val dataset = LegacyDatasetFactory.createFillDataset(
            request.usernameId, request.passwordId, request.otpId,
            basicCred.username, basicCred.password, totpCode
        )

        if (dataset != null) {
            autofillUseCases.recordUsage(candidate.candidateId)
            val payload = if (request.returnsDataset) {
                AutofillAuthenticationPayload.DatasetResult(dataset)
            } else {
                AutofillAuthenticationPayload.Response(
                    FillResponse.Builder().addDataset(dataset).build()
                )
            }
            mutate(AutofillFillMutation.Completed(payload))
        } else {
            mutate(AutofillFillMutation.Failed("No fillable fields detected"))
        }
    }

    private fun selectCandidate(candidate: ResolvedCandidate) {
        val request = currentRequest ?: return
        viewModelScope.launch {
            mutate(AutofillFillMutation.Loading)
            val settings = settingsRepository.settings.first().interaction.autofill
            if (!ensureAuthenticatedForSecretAccess(settings)) return@launch
            val resolved = loadSelectedCandidate(
                candidate.candidateId,
                request,
                settings,
            )
            if (resolved == null) {
                mutate(AutofillFillMutation.Failed("Entry not found"))
                return@launch
            }
            handleSingleEntry(resolved, request)
        }
    }

    /**
     * 候选列表只持有展示字段。认证完成后才按 ID 读取并解密被选中的单条凭据。
     */
    private suspend fun loadSelectedCandidate(
        entryId: String?,
        request: AutofillFillRequest,
        settings: AutofillSettings,
    ): ResolvedCandidate? = entryId?.let {
        candidateResolver.resolveSelected(
            entryId = it,
            packageName = request.packageName,
            webDomain = request.webDomain,
            settings = settings,
        )
    }

    private suspend fun ensureAuthenticatedForSecretAccess(
        settings: AutofillSettings,
    ): Boolean {
        val needsAuthentication = !vaultAccessState.hasFullSecureSessionAccess() ||
            (settings.requireAuthentication && !authenticatedForCurrentRequest)
        if (!needsAuthentication) return true
        val authResult = authenticateForAutofill()
        if (authResult !is AuthenticationResult.Success) {
            mutate(AutofillFillMutation.Completed(null))
            return false
        }
        authenticatedForCurrentRequest = true
        return true
    }

    private suspend fun authenticateForAutofill(): AuthenticationResult =
        requestSession.authenticate()

    private fun mutate(mutation: AutofillFillMutation) {
        _uiState.value = AutofillFillReducer.reduce(_uiState.value, mutation)
    }

    suspend fun closeRequestSession() {
        requestSession.close()
    }
}

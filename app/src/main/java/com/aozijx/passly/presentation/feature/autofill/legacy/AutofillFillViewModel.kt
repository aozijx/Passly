package com.aozijx.passly.presentation.feature.autofill.legacy

import android.content.Context
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.autofill.internal.CandidateRetriever
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyDatasetFactory
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.shared.AutofillRequestSession
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillFillViewModel @Inject constructor(
    private val recordAutofillUsage: RecordAutofillUsageUseCase,
    private val candidateRetriever: CandidateRetriever,
    private val vaultAccessState: SecureSessionAccessState,
    private val settingsRepository: AppSettingsRepository,
    private val requestSession: AutofillRequestSession,
    private val grantStore: AutofillGrantStore,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutofillFillUiState>(AutofillFillUiState.Initial)
    val uiState: StateFlow<AutofillFillUiState> = _uiState.asStateFlow()

    private var currentRequest: AutofillFillRequest? = null
    private var authenticatedForCurrentRequest = false

    fun onAction(action: AutofillFillUiAction) {
        when (action) {
            is AutofillFillUiAction.Initialize -> initialize(action.request)
            is AutofillFillUiAction.CandidateSelected -> selectCandidate(action.candidate)
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
                    val candidates = candidateRetriever.resolveByIds(
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
            } catch (e: CancellationException) {
                throw e
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
        val candidates = candidateRetriever.resolveByPackage(
            request.packageName,
            request.webDomain,
            settings,
            includeSecrets = false,
        )
        val roleIds = mutableMapOf<FieldRole, List<AutofillId>>()
        if (request.usernameIds.isNotEmpty()) roleIds[FieldRole.USERNAME] = request.usernameIds
        if (request.passwordIds.isNotEmpty()) roleIds[FieldRole.PASSWORD] = request.passwordIds
        if (request.otpIds.isNotEmpty()) roleIds[FieldRole.OTP] = request.otpIds

        val response = LegacyResponseFactory.buildCandidateAuthenticationResponse(
            appContext,
            candidates = candidates,
            editableIds = request.editableIds,
            roleIds = roleIds,
            packageName = request.packageName,
            webDomain = request.webDomain,
            uiMode = request.uiMode,
            savePromptsEnabled = settings.savePromptsEnabled,
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
        val basicCred = candidate.fillableCredentials()
        if (basicCred == null) {
            mutate(AutofillFillMutation.Failed("Failed to decrypt credentials"))
            return
        }

        val totpCode = if (request.otpIds.isNotEmpty()) candidate.entry.otpPreview else null

        val dataset = LegacyDatasetFactory.createFillDatasetForRoles(
            usernameIds = request.usernameIds,
            passwordIds = request.passwordIds,
            otpIds = request.otpIds,
            username = basicCred.username,
            password = basicCred.password,
            totpCode = totpCode,
        )

        if (dataset != null) {
            recordAutofillUsage(candidate.entry.id.value)
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
                candidate.entry.id.value,
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

    private suspend fun loadSelectedCandidate(
        entryId: String?,
        request: AutofillFillRequest,
        settings: AutofillSettings,
    ): ResolvedCandidate? = entryId?.let {
        candidateRetriever.resolveSelected(
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
                (settings.requireAuthentication && !authenticatedForCurrentRequest &&
                        !grantActiveForCurrentRequest())
        if (!needsAuthentication) return true
        val authResult = authenticateForAutofill()
        if (authResult !is AuthenticationResult.Success) {
            mutate(AutofillFillMutation.Completed(null))
            return false
        }
        authenticatedForCurrentRequest = true
        grantForCurrentRequest()
        return true
    }

    private suspend fun authenticateForAutofill(): AuthenticationResult =
        requestSession.authenticate().also { result ->
            if (result is AuthenticationResult.Success) grantForCurrentRequest()
        }

    private fun grantActiveForCurrentRequest(): Boolean {
        val request = currentRequest ?: return false
        val packageName = request.packageName?.takeIf(String::isNotBlank) ?: return false
        return grantStore.isGranted(
            AutofillGrantContext(packageName = packageName, webDomain = request.webDomain)
        )
    }

    private fun grantForCurrentRequest() {
        val request = currentRequest ?: return
        val packageName = request.packageName?.takeIf(String::isNotBlank) ?: return
        grantStore.grant(
            AutofillGrantContext(packageName = packageName, webDomain = request.webDomain)
        )
    }

    private fun mutate(mutation: AutofillFillMutation) {
        _uiState.value = AutofillFillReducer.reduce(_uiState.value, mutation)
    }

    suspend fun closeRequestSession() {
        requestSession.close()
    }

    override fun onCleared() {
        viewModelScope.launch {
            requestSession.close()
        }
    }
}

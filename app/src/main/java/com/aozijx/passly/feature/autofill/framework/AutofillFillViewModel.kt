package com.aozijx.passly.feature.autofill.framework

import android.content.Context
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.autofill.usecase.AutofillUseCases
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.service.autofill.framework.builder.LegacyDatasetFactory
import com.aozijx.passly.service.autofill.framework.builder.LegacyResponseFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AutofillAuthenticationPayload {
    data class Response(val value: FillResponse) : AutofillAuthenticationPayload
    data class DatasetResult(val value: Dataset) : AutofillAuthenticationPayload
}

@HiltViewModel
class AutofillFillViewModel @Inject constructor(
    private val autofillUseCases: AutofillUseCases,
    private val candidateResolver: CandidateResolver,
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: VaultAccessState,
    private val settingsRepository: AppSettingsRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class ShowCandidates(val candidates: List<ResolvedCandidate>) : UiState()
        data class Result(val payload: AutofillAuthenticationPayload?) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentRequest: FillRequest? = null
    private var authenticatedForCurrentRequest = false

    data class FillRequest(
        val uiMode: AutofillPresentation,
        val isUnlockOnly: Boolean,
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val otpId: AutofillId?,
        val packageName: String?,
        val webDomain: String?,
        val directEntryId: String?,
        val candidateEntryIds: List<String>,
        val returnsDataset: Boolean,
    )

    fun initialize(request: FillRequest) {
        currentRequest = request
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            try {
                val settings = settingsRepository.settings.first().interaction.autofill
                if (request.isUnlockOnly) {
                    handleUnlockOnly(request, settings)
                    return@launch
                }
                if (vaultAccessState.isLocked()) {
                    val authentication = authenticateForAutofill()
                    if (authentication !is AuthenticationResult.Success) {
                        _uiState.update { UiState.Result(null) }
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
                        _uiState.update { UiState.Error("No matching entries") }
                    } else {
                        _uiState.update { UiState.ShowCandidates(candidates) }
                    }
                } else {
                    val candidate = request.directEntryId
                        ?.let {
                            candidateResolver.resolveSelected(
                                entryId = it,
                                packageName = request.packageName,
                                webDomain = request.webDomain,
                                settings = settings,
                            )
                        }
                    if (candidate == null) {
                        _uiState.update { UiState.Error("Entry not found") }
                    } else {
                        handleSingleEntry(candidate, request, settings)
                    }
                }
            } catch (e: Exception) {
                AppTelemetry.e("AutofillVM", "Error", e)
                _uiState.update { UiState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    private suspend fun handleUnlockOnly(
        request: FillRequest,
        settings: AutofillSettings,
    ) {
        val authResult = authenticateForAutofill()
        if (authResult !is AuthenticationResult.Success) {
            _uiState.update { UiState.Result(null) }
            return
        }
        authenticatedForCurrentRequest = true
        val candidates = candidateResolver.resolveByPackage(
            request.packageName,
            request.webDomain,
            settings,
        )
        if (candidates.isEmpty()) {
            _uiState.update { UiState.Result(null) }
            return
        }
        val response = LegacyResponseFactory.buildPostUnlockFillResponse(
            appContext,
            candidates = candidates,
            usernameId = request.usernameId,
            passwordId = request.passwordId,
            otpId = request.otpId
        )
        _uiState.update {
            UiState.Result(response?.let(AutofillAuthenticationPayload::Response))
        }
    }

    private suspend fun handleSingleEntry(
        candidate: ResolvedCandidate,
        request: FillRequest,
        settings: AutofillSettings,
    ) {
        val needsAuthentication = vaultAccessState.isLocked() ||
                (settings.requireAuthentication && !authenticatedForCurrentRequest)
        if (needsAuthentication) {
            val authResult = authenticateForAutofill()
            if (authResult !is AuthenticationResult.Success) {
                _uiState.update { UiState.Result(null) }
                return
            }
            authenticatedForCurrentRequest = true
        }

        val basicCred = LegacyResponseFactory.getBasicCredentials(candidate)
        if (basicCred == null) {
            _uiState.update { UiState.Error("Failed to decrypt credentials") }
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
            _uiState.update { UiState.Result(payload) }
        } else {
            _uiState.update { UiState.Error("No fillable fields detected") }
        }
    }

    fun selectCandidate(candidate: ResolvedCandidate) {
        val request = currentRequest ?: return
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            val settings = settingsRepository.settings.first().interaction.autofill
            handleSingleEntry(candidate, request, settings)
        }
    }

    private suspend fun authenticateForAutofill(): AuthenticationResult =
        authenticationManager.authenticate(AuthenticationRequest(AuthenticationPurpose.AUTOFILL))
}

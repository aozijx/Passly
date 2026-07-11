package com.aozijx.passly.ui.features.autofill.framework

import android.content.Context
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.VerificationGateway
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.service.autofill.framework.builder.LegacyDatasetFactory
import com.aozijx.passly.service.autofill.framework.builder.LegacyResponseFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class AutofillFillViewModel @Inject constructor(
    private val autofillUseCases: AutofillUseCases,
    private val candidateResolver: CandidateResolver,
    private val verificationGateway: VerificationGateway,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class ShowCandidates(val candidates: List<ResolvedCandidate>) : UiState()
        data class Result(val response: FillResponse?) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentRequest: FillRequest? = null

    data class FillRequest(
        val uiMode: AutofillUiMode,
        val isUnlockOnly: Boolean,
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val otpId: AutofillId?,
        val packageName: String?,
        val webDomain: String?,
        val directEntryId: Int?,
        val candidateEntryIds: List<Int>,
    )

    fun initialize(request: FillRequest, biometricLauncher: BiometricPromptLauncher) {
        currentRequest = request
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            try {
                if (request.isUnlockOnly) {
                    handleUnlockOnly(request, biometricLauncher)
                } else if (request.uiMode == AutofillUiMode.BOTTOM_SHEET && request.candidateEntryIds.isNotEmpty() && request.directEntryId == null) {
                    val candidates = candidateResolver.resolveByIds(request.candidateEntryIds)
                    if (candidates.isEmpty()) {
                        _uiState.update { UiState.Error("No matching entries") }
                    } else {
                        _uiState.update { UiState.ShowCandidates(candidates) }
                    }
                } else {
                    val candidate = request.directEntryId
                        ?.let { candidateResolver.resolveByIds(listOf(it)).firstOrNull() }
                    if (candidate == null) {
                        _uiState.update { UiState.Error("Entry not found") }
                    } else {
                        handleSingleEntry(candidate, request, biometricLauncher)
                    }
                }
            } catch (e: Exception) {
                Logcat.Companion.e("AutofillVM", "Error", e)
                _uiState.update { UiState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    private suspend fun handleUnlockOnly(request: FillRequest, launcher: BiometricPromptLauncher) {
        val authResult = verifyWithBiometricSuspended(
            launcher,
            "解锁保险库",
            "请验证身份以自动填充"
        )
        if (authResult is AppResult.Failure) {
            _uiState.update { UiState.Result(null) }
            return
        }
        val candidates = candidateResolver.resolveByPackage(request.packageName, request.webDomain)
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
        _uiState.update { UiState.Result(response) }
    }

    private suspend fun handleSingleEntry(
        candidate: ResolvedCandidate,
        request: FillRequest,
        launcher: BiometricPromptLauncher
    ) {
        val authResult = verifyWithBiometricSuspended(
            launcher,
            "确认填充",
            "请验证身份以填充此凭证"
        )
        if (authResult is AppResult.Failure) {
            _uiState.update { UiState.Result(null) }
            return
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
            autofillUseCases.updateLastUsed(candidate.candidateId)
            val response = FillResponse.Builder().addDataset(dataset).build()
            _uiState.update { UiState.Result(response) }
        } else {
            _uiState.update { UiState.Error("No fillable fields detected") }
        }
    }

    fun selectCandidate(candidate: ResolvedCandidate, biometricLauncher: BiometricPromptLauncher) {
        val request = currentRequest ?: return
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            handleSingleEntry(candidate, request, biometricLauncher)
        }
    }

    private suspend fun verifyWithBiometricSuspended(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String,
    ): AppResult<Unit> = suspendCancellableCoroutine { continuation ->
        verificationGateway.verifyWithBiometric(launcher, title, subtitle) { result ->
            continuation.resume(result)
        }
    }
}

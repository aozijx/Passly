package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeCommitResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeCopyResult
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecoveryCodeSettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val draftInteractor: RecoveryCodeDraftInteractor,
    private val authenticationSettings: SecurityAuthenticationSettingsInteractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RecoveryCodeSettingsUiState(
            draft = if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) {
                RecoveryCodeDraftUiState.Expired
            } else {
                RecoveryCodeDraftUiState.Empty
            },
        ),
    )
    val uiState: StateFlow<RecoveryCodeSettingsUiState> = _uiState.asStateFlow()
    private val _effects = Channel<RecoveryCodeSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(hasRecoveryCode = authenticationSettings.hasRecoveryCode()) }
        }
    }

    fun onAction(action: RecoveryCodeSettingsAction) {
        when (action) {
            RecoveryCodeSettingsAction.Generate -> generate()
            RecoveryCodeSettingsAction.Copy -> copy()
            RecoveryCodeSettingsAction.ConfirmAndEnable -> confirmAndEnable()
            RecoveryCodeSettingsAction.DismissDisclosure -> dismissDisclosure()
            is RecoveryCodeSettingsAction.VerificationInputChanged ->
                updateVerificationInput(action.value)
            RecoveryCodeSettingsAction.Verify -> verify()
            RecoveryCodeSettingsAction.ClearVerificationResult -> clearVerificationResult()
        }
    }

    private fun generate() {
        if (_uiState.value.draft is RecoveryCodeDraftUiState.Creating) return
        viewModelScope.launch {
            replaceDraft(RecoveryCodeDraftUiState.Creating)
            savedStateHandle[WAS_DISCLOSURE_OPEN] = false
            try {
                when (val result = draftInteractor.generate()) {
                    is RecoveryCodeDraftResult.Ready -> {
                        savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                        replaceDraft(RecoveryCodeDraftUiState.Ready(result.disclosure))
                    }
                    RecoveryCodeDraftResult.Cancelled ->
                        replaceDraft(RecoveryCodeDraftUiState.Empty)
                    RecoveryCodeDraftResult.Failed ->
                        replaceDraft(RecoveryCodeDraftUiState.Failed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                replaceDraft(RecoveryCodeDraftUiState.Failed)
            }
        }
    }

    private fun copy() {
        viewModelScope.launch {
            when (draftInteractor.copy()) {
                RecoveryCodeCopyResult.Copied ->
                    _effects.send(RecoveryCodeSettingsEffect.Copied)
                RecoveryCodeCopyResult.NoActiveDraft -> Unit
            }
        }
    }

    private fun confirmAndEnable() {
        viewModelScope.launch {
            when (draftInteractor.commit()) {
                RecoveryCodeCommitResult.Committed -> {
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    replaceDraft(RecoveryCodeDraftUiState.Committed)
                    _uiState.update { it.copy(hasRecoveryCode = true) }
                }
                RecoveryCodeCommitResult.Failed,
                RecoveryCodeCommitResult.NoActiveDraft -> {
                    draftInteractor.dismiss()
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    replaceDraft(RecoveryCodeDraftUiState.Failed)
                }
            }
        }
    }

    private fun dismissDisclosure() {
        draftInteractor.dismiss()
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        replaceDraft(RecoveryCodeDraftUiState.Empty)
    }

    private fun updateVerificationInput(value: String) {
        val previous = _uiState.value.verification.input
        val replacement = OwnedChars.fromString(value)
        _uiState.update {
            it.copy(verification = RecoveryCodeVerificationUiState.Editing(replacement))
        }
        previous.wipe()
    }

    private fun verify() {
        val current = _uiState.value.verification
        if (current is RecoveryCodeVerificationUiState.Verifying || current.input.isEmpty) return
        viewModelScope.launch {
            val input = _uiState.value.verification.input
            val code = input.toCharArray()
            _uiState.update {
                it.copy(verification = RecoveryCodeVerificationUiState.Verifying(input))
            }
            try {
                val valid = authenticationSettings.verifyRecoveryCode(code)
                _uiState.update {
                    it.copy(
                        verification = if (valid) {
                            RecoveryCodeVerificationUiState.Accepted(input)
                        } else {
                            RecoveryCodeVerificationUiState.Rejected(input)
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                _uiState.update {
                    it.copy(verification = RecoveryCodeVerificationUiState.Editing(input))
                }
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(verification = RecoveryCodeVerificationUiState.Failed(input))
                }
            } finally {
                code.fill('\u0000')
            }
        }
    }

    private fun clearVerificationResult() {
        val current = _uiState.value.verification
        if (current is RecoveryCodeVerificationUiState.Verifying ||
            current is RecoveryCodeVerificationUiState.Editing
        ) return
        _uiState.update {
            it.copy(verification = RecoveryCodeVerificationUiState.Editing(current.input))
        }
    }

    private fun replaceDraft(draft: RecoveryCodeDraftUiState) {
        val previous = (_uiState.value.draft as? RecoveryCodeDraftUiState.Ready)?.disclosure
        _uiState.update { it.copy(draft = draft) }
        val current = (draft as? RecoveryCodeDraftUiState.Ready)?.disclosure
        if (previous !== current) previous?.wipe()
    }

    override fun onCleared() {
        (_uiState.value.draft as? RecoveryCodeDraftUiState.Ready)?.disclosure?.wipe()
        _uiState.value.verification.input.wipe()
        draftInteractor.close()
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
    }
}

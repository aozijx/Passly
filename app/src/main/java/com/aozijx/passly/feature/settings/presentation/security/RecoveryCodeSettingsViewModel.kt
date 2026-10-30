package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeCommitResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeCopyResult
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val pageEffects = PageEffects<RecoveryCodeSettingsEffect>()
    val effects = pageEffects.effects

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
        if (_uiState.value.draft is RecoveryCodeDraftUiState.Creating ||
            _uiState.value.draft is RecoveryCodeDraftUiState.Committing
        ) return
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
        if (_uiState.value.draft !is RecoveryCodeDraftUiState.Ready) return
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            when (draftInteractor.copy()) {
                RecoveryCodeCopyResult.Copied ->
                    emitEffect(owner, RecoveryCodeSettingsEffect.Copied)
                RecoveryCodeCopyResult.NoActiveDraft -> Unit
            }
        }
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }

    private fun emitEffect(owner: Any, effect: RecoveryCodeSettingsEffect) {
        pageEffects.tryEmit(owner, effect)
    }

    private fun confirmAndEnable() {
        val ready = _uiState.value.draft as? RecoveryCodeDraftUiState.Ready ?: return
        replaceDraft(RecoveryCodeDraftUiState.Committing(ready.disclosure))
        viewModelScope.launch {
            val result = try {
                draftInteractor.commit()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                RecoveryCodeCommitResult.Failed
            }
            when (result) {
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
        if (_uiState.value.draft is RecoveryCodeDraftUiState.Committing) return
        draftInteractor.dismiss()
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        replaceDraft(RecoveryCodeDraftUiState.Empty)
    }

    private fun updateVerificationInput(value: String) {
        if (_uiState.value.verification is RecoveryCodeVerificationUiState.Verifying) return
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
        val previous = _uiState.value.draft.disclosureOrNull()
        _uiState.update { it.copy(draft = draft) }
        val current = draft.disclosureOrNull()
        if (previous !== current) previous?.wipe()
    }

    override fun onCleared() {
        _uiState.value.draft.disclosureOrNull()?.wipe()
        _uiState.value.verification.input.wipe()
        draftInteractor.close()
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
    }
}

private fun RecoveryCodeDraftUiState.disclosureOrNull() = when (this) {
    is RecoveryCodeDraftUiState.Ready -> disclosure
    is RecoveryCodeDraftUiState.Committing -> disclosure
    else -> null
}

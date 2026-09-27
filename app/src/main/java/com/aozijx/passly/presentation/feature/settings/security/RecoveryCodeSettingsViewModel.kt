package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftResult
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
            draftStatus = if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) {
                RecoveryCodeDraftStatus.EXPIRED
            } else {
                RecoveryCodeDraftStatus.EMPTY
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
            RecoveryCodeSettingsAction.ClearVerificationResult ->
                _uiState.update { it.copy(verificationResult = null) }
        }
    }

    private fun generate() {
        if (_uiState.value.draftStatus == RecoveryCodeDraftStatus.CREATING) return
        viewModelScope.launch {
            clearDisclosure()
            savedStateHandle[WAS_DISCLOSURE_OPEN] = false
            _uiState.update { it.copy(draftStatus = RecoveryCodeDraftStatus.CREATING) }
            try {
                when (val result = draftInteractor.generate()) {
                    is RecoveryCodeDraftResult.Ready -> {
                        savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                        _uiState.update {
                            it.copy(
                                draftStatus = RecoveryCodeDraftStatus.READY,
                                disclosure = result.disclosure,
                            )
                        }
                    }
                    RecoveryCodeDraftResult.Cancelled -> _uiState.update {
                        it.copy(draftStatus = RecoveryCodeDraftStatus.EMPTY)
                    }
                    RecoveryCodeDraftResult.Failed -> _uiState.update {
                        it.copy(draftStatus = RecoveryCodeDraftStatus.FAILED)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { it.copy(draftStatus = RecoveryCodeDraftStatus.FAILED) }
            }
        }
    }

    private fun copy() {
        viewModelScope.launch {
            if (draftInteractor.copy()) _effects.send(RecoveryCodeSettingsEffect.Copied)
        }
    }

    private fun confirmAndEnable() {
        viewModelScope.launch {
            if (draftInteractor.commit() == true) {
                clearDisclosure()
                savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                _uiState.update {
                    it.copy(
                        hasRecoveryCode = true,
                        draftStatus = RecoveryCodeDraftStatus.COMMITTED,
                    )
                }
            } else {
                draftInteractor.dismiss()
                clearDisclosure()
                savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                _uiState.update { it.copy(draftStatus = RecoveryCodeDraftStatus.FAILED) }
            }
        }
    }

    private fun dismissDisclosure() {
        draftInteractor.dismiss()
        clearDisclosure()
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        _uiState.update { it.copy(draftStatus = RecoveryCodeDraftStatus.EMPTY) }
    }

    private fun updateVerificationInput(value: String) {
        _uiState.value.verificationInput.wipe()
        _uiState.update {
            it.copy(
                verificationInput = OwnedChars.fromString(value),
                verificationResult = null,
            )
        }
    }

    private fun verify() {
        if (_uiState.value.isVerifying || _uiState.value.verificationInput.isEmpty) return
        viewModelScope.launch {
            val code = _uiState.value.verificationInput.toCharArray()
            _uiState.update { it.copy(isVerifying = true, verificationResult = null) }
            try {
                val valid = authenticationSettings.verifyRecoveryCode(code)
                _uiState.update { it.copy(verificationResult = valid) }
            } finally {
                code.fill('\u0000')
                _uiState.update { it.copy(isVerifying = false) }
            }
        }
    }

    private fun clearDisclosure() {
        _uiState.value.disclosure.wipe()
        _uiState.update { it.copy(disclosure = EmptySensitiveValue) }
    }

    override fun onCleared() {
        _uiState.value.disclosure.wipe()
        _uiState.value.verificationInput.wipe()
        draftInteractor.close()
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
    }
}

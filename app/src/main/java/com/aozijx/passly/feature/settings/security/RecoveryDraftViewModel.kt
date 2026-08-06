package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.RecoveryCodeDraft
import com.aozijx.passly.domain.authentication.RecoveryCodeDraftCreation
import com.aozijx.passly.domain.authentication.RecoveryCodeDraftFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
class RecoveryDraftViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authenticationManager: AuthenticationManager,
    private val draftFactory: RecoveryCodeDraftFactory
) : ViewModel() {
    private var draft: RecoveryCodeDraft? = null
    private val _state = MutableStateFlow<RecoveryDraftState>(
        if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) RecoveryDraftState.DraftExpired
        else RecoveryDraftState.Empty
    )
    val state: StateFlow<RecoveryDraftState> = _state.asStateFlow()

    fun generate() {
        if (authenticationManager.state.value !is AuthenticationState.Authenticated) {
            _state.value = RecoveryDraftState.Failed
            return
        }
        if (_state.value is RecoveryDraftState.Authenticating ||
            _state.value is RecoveryDraftState.Generating
        ) return
        viewModelScope.launch {
            _state.value = RecoveryDraftState.Authenticating
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.MANAGE_RECOVERY_CODE)
                )
            ) {
                is AuthenticationResult.Success -> createDraft()
                is AuthenticationResult.Cancelled -> _state.value = RecoveryDraftState.Empty
                is AuthenticationResult.Failure -> _state.value = RecoveryDraftState.Failed
            }
        }
    }

    fun revealCode(): CharArray? =
        if (authenticationManager.state.value is AuthenticationState.Authenticated) {
            draft?.reveal()
        } else {
            null
        }

    fun confirmAndEnable() {
        viewModelScope.launch {
            if (authenticationManager.state.value !is AuthenticationState.Authenticated) {
                dismiss()
                _state.value = RecoveryDraftState.Failed
                return@launch
            }
            val activeDraft = draft ?: return@launch
            when (activeDraft.commit()) {
                is AuthenticationResult.Success -> {
                    draft = null
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    savedStateHandle[DRAFT_GENERATION_ID] = null as String?
                    _state.value = RecoveryDraftState.Committed
                }
                is AuthenticationResult.Cancelled,
                is AuthenticationResult.Failure -> _state.value = RecoveryDraftState.Failed
            }
        }
    }

    fun dismiss() {
        draft?.clear()
        draft = null
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        savedStateHandle[DRAFT_GENERATION_ID] = null as String?
        _state.value = RecoveryDraftState.Empty
    }

    private suspend fun createDraft() {
        _state.value = RecoveryDraftState.Generating
        try {
            when (val creation = draftFactory.create()) {
                is RecoveryCodeDraftCreation.Ready -> {
                    draft?.clear()
                    draft = creation.draft
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                    savedStateHandle[DRAFT_GENERATION_ID] = creation.draft.generationId
                    _state.value = RecoveryDraftState.Ready(creation.draft.generationId)
                }
                is RecoveryCodeDraftCreation.Failed -> _state.value = RecoveryDraftState.Failed
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            _state.value = RecoveryDraftState.Failed
        }
    }

    override fun onCleared() {
        draft?.clear()
        draft = null
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
        const val DRAFT_GENERATION_ID = "recoveryDraftGenerationId"
    }
}

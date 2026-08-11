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
import com.aozijx.passly.feature.settings.security.presentation.RecoveryDraftMutation
import com.aozijx.passly.feature.settings.security.presentation.RecoveryDraftReducer
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

    fun onAction(action: RecoveryDraftAction) {
        when (action) {
            RecoveryDraftAction.Generate -> generateDraft()
            RecoveryDraftAction.ConfirmAndEnable -> confirmAndEnable()
            RecoveryDraftAction.Dismiss -> dismissDraft()
        }
    }

    private fun generateDraft() {
        if (authenticationManager.state.value !is AuthenticationState.Authenticated) {
            mutate(RecoveryDraftMutation.Failed)
            return
        }
        if (_state.value is RecoveryDraftState.Authenticating ||
            _state.value is RecoveryDraftState.Generating
        ) return
        viewModelScope.launch {
            mutate(RecoveryDraftMutation.AuthenticationStarted)
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.MANAGE_RECOVERY_CODE)
                )
            ) {
                is AuthenticationResult.Success -> createDraft()
                is AuthenticationResult.Cancelled ->
                    mutate(RecoveryDraftMutation.AuthenticationCancelled)
                is AuthenticationResult.Failure -> mutate(RecoveryDraftMutation.Failed)
            }
        }
    }

    fun revealCode(): CharArray? =
        if (authenticationManager.state.value is AuthenticationState.Authenticated) {
            draft?.reveal()
        } else {
            null
        }

    private fun confirmAndEnable() {
        viewModelScope.launch {
            if (authenticationManager.state.value !is AuthenticationState.Authenticated) {
                clearDraft()
                mutate(RecoveryDraftMutation.Failed)
                return@launch
            }
            val activeDraft = draft ?: return@launch
            when (activeDraft.commit()) {
                is AuthenticationResult.Success -> {
                    draft = null
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    savedStateHandle[DRAFT_GENERATION_ID] = null as String?
                    mutate(RecoveryDraftMutation.Committed)
                }
                is AuthenticationResult.Cancelled,
                is AuthenticationResult.Failure -> mutate(RecoveryDraftMutation.Failed)
            }
        }
    }

    private fun dismissDraft() {
        clearDraft()
        mutate(RecoveryDraftMutation.Dismissed)
    }

    private fun clearDraft() {
        draft?.clear()
        draft = null
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        savedStateHandle[DRAFT_GENERATION_ID] = null as String?
    }

    private suspend fun createDraft() {
        mutate(RecoveryDraftMutation.GenerationStarted)
        try {
            when (val creation = draftFactory.create()) {
                is RecoveryCodeDraftCreation.Ready -> {
                    draft?.clear()
                    draft = creation.draft
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                    savedStateHandle[DRAFT_GENERATION_ID] = creation.draft.generationId
                    mutate(RecoveryDraftMutation.DraftReady(creation.draft.generationId))
                }
                is RecoveryCodeDraftCreation.Failed -> mutate(RecoveryDraftMutation.Failed)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            mutate(RecoveryDraftMutation.Failed)
        }
    }

    private fun mutate(mutation: RecoveryDraftMutation) {
        _state.value = RecoveryDraftReducer.reduce(_state.value, mutation)
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

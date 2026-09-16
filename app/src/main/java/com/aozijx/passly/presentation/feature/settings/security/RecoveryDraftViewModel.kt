package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryDraftViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val draftFactory: RecoveryCredentialFactory,
    private val clipboardWriter: SensitiveClipboardWriter,
) : ViewModel() {
    private var draft: RecoveryCredentialDraft? = null
    private val _state = MutableStateFlow<RecoveryDraftState>(
        if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) RecoveryDraftState.DraftExpired
        else RecoveryDraftState.Empty
    )
    val state: StateFlow<RecoveryDraftState> = _state.asStateFlow()
    private val _effects = Channel<RecoveryDraftEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: RecoveryDraftAction) {
        when (action) {
            RecoveryDraftAction.Generate -> generateDraft()
            RecoveryDraftAction.ConfirmAndEnable -> confirmAndEnable()
            RecoveryDraftAction.Dismiss -> dismissDraft()
            RecoveryDraftAction.Copy -> copyDraft()
        }
    }

    private fun generateDraft() {
        if (_state.value is RecoveryDraftState.Creating) return
        viewModelScope.launch {
            mutate(RecoveryDraftMutation.CreationStarted)
            try {
                when (val creation = draftFactory.create()) {
                    is RecoveryCredentialCreation.Ready -> {
                        draft?.close()
                        draft = creation.draft
                        savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                        mutate(RecoveryDraftMutation.DraftReady(creation.draft.id.value))
                    }
                    is RecoveryCredentialCreation.Failed -> mutate(RecoveryDraftMutation.Failed)
                    RecoveryCredentialCreation.Cancelled ->
                        mutate(RecoveryDraftMutation.CreationCancelled)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                mutate(RecoveryDraftMutation.Failed)
            }
        }
    }

    fun revealCode(): CharArray? = draft?.reveal()

    private fun copyDraft() {
        viewModelScope.launch {
            val chars = draft?.reveal() ?: return@launch
            try {
                clipboardWriter.writeSensitive(String(chars))
                _effects.send(RecoveryDraftEffect.Copied)
            } finally {
                chars.fill('\u0000')
            }
        }
    }
    private fun confirmAndEnable() {
        viewModelScope.launch {
            val activeDraft = draft ?: return@launch
            when (activeDraft.commit()) {
                is AuthenticationResult.Success -> {
                    draft = null
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
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
        draft?.close()
        draft = null
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
    }

    private fun mutate(mutation: RecoveryDraftMutation) {
        _state.value = RecoveryDraftReducer.reduce(_state.value, mutation)
    }

    override fun onCleared() {
        draft?.close()
        draft = null
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
    }
}

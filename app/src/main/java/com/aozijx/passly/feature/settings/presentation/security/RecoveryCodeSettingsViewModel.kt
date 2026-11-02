package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.settings.security.RecoveryCodeCommitResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeCopyResult
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftInteractor
import com.aozijx.passly.feature.settings.security.RecoveryCodeDraftResult
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RecoveryCodeSettingsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val draftInteractor: RecoveryCodeDraftInteractor,
        private val authenticationSettings: SecurityAuthenticationSettingsInteractor,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                RecoveryCodeSettingsUiState(
                    draft =
                        if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) {
                            RecoveryCodeDraftUiState.Expired
                        } else {
                            RecoveryCodeDraftUiState.Empty
                        },
                ),
            )
        val uiState: StateFlow<RecoveryCodeSettingsUiState> = _uiState.asStateFlow()
        private val pageEffects = PageEffects<RecoveryCodeSettingsEffect>()
        val effects = pageEffects.effects
        private var generationJob: Job? = null
        private var verificationJob: Job? = null
        private var verificationRequest: Any? = null
        private var commitInFlight = false

        init {
            viewModelScope.launch {
                _uiState.update { it.copy(hasRecoveryCode = authenticationSettings.hasRecoveryCode()) }
            }
        }

        fun onAction(action: RecoveryCodeSettingsAction) {
            if (pageEffects.currentOwner == null) return
            when (action) {
                RecoveryCodeSettingsAction.Generate -> {
                    generate()
                }

                RecoveryCodeSettingsAction.Copy -> {
                    copy()
                }

                RecoveryCodeSettingsAction.ConfirmAndEnable -> {
                    confirmAndEnable()
                }

                RecoveryCodeSettingsAction.DismissDisclosure -> {
                    dismissDisclosure()
                }

                is RecoveryCodeSettingsAction.VerificationInputChanged -> {
                    updateVerificationInput(action.value)
                }

                RecoveryCodeSettingsAction.Verify -> {
                    verify()
                }

                RecoveryCodeSettingsAction.ClearVerificationResult -> {
                    clearVerificationResult()
                }
            }
        }

        private fun generate() {
            val owner = pageEffects.currentOwner ?: return
            if (_uiState.value.draft is RecoveryCodeDraftUiState.Creating ||
                _uiState.value.draft is RecoveryCodeDraftUiState.Committing || commitInFlight
            ) {
                return
            }
            generationJob =
                viewModelScope.launch {
                    replaceDraft(RecoveryCodeDraftUiState.Creating)
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    try {
                        when (val result = draftInteractor.generate()) {
                            is RecoveryCodeDraftResult.Ready -> {
                                if (!pageEffects.isActive(owner)) {
                                    result.disclosure.wipe()
                                    draftInteractor.dismiss()
                                    return@launch
                                }
                                savedStateHandle[WAS_DISCLOSURE_OPEN] = true
                                replaceDraft(RecoveryCodeDraftUiState.Ready(result.disclosure))
                            }

                            RecoveryCodeDraftResult.Cancelled -> {
                                if (pageEffects.isActive(owner)) {
                                    replaceDraft(RecoveryCodeDraftUiState.Empty)
                                }
                            }

                            RecoveryCodeDraftResult.Failed -> {
                                if (pageEffects.isActive(owner)) {
                                    replaceDraft(RecoveryCodeDraftUiState.Failed)
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (pageEffects.isActive(owner)) replaceDraft(RecoveryCodeDraftUiState.Failed)
                    } finally {
                        if (generationJob === coroutineContext[Job]) generationJob = null
                    }
                }
        }

        private fun copy() {
            if (_uiState.value.draft !is RecoveryCodeDraftUiState.Ready) return
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                when (draftInteractor.copy()) {
                    RecoveryCodeCopyResult.Copied -> {
                        emitEffect(owner, RecoveryCodeSettingsEffect.Copied)
                    }

                    RecoveryCodeCopyResult.NoActiveDraft -> {
                        Unit
                    }
                }
            }
        }

        fun onPageEntered(): Any {
            pageEffects.currentOwner?.let(::onPageExited)
            return pageEffects.enter()
        }

        fun onPageExited(owner: Any) {
            if (!pageEffects.isActive(owner)) return
            pageEffects.exit(owner)
            generationJob?.cancel()
            generationJob = null
            verificationRequest = null
            verificationJob?.cancel()
            verificationJob = null

            val previousInput = _uiState.value.verification.input
            _uiState.update { it.copy(verification = RecoveryCodeVerificationUiState.Editing(OwnedChars.EMPTY)) }
            previousInput.wipe()

            when (val draft = _uiState.value.draft) {
                is RecoveryCodeDraftUiState.Ready -> {
                    draftInteractor.dismiss()
                    clearDisclosureMarker()
                    replaceDraft(RecoveryCodeDraftUiState.Empty)
                }

                is RecoveryCodeDraftUiState.Committing -> {
                    clearDisclosureMarker()
                    replaceDraft(draft.copy(disclosure = null))
                }

                RecoveryCodeDraftUiState.Creating -> {
                    draftInteractor.dismiss()
                    clearDisclosureMarker()
                    replaceDraft(RecoveryCodeDraftUiState.Empty)
                }

                else -> {
                    clearDisclosureMarker()
                    replaceDraft(RecoveryCodeDraftUiState.Empty)
                }
            }
        }

        private fun emitEffect(
            owner: Any,
            effect: RecoveryCodeSettingsEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }

        private fun confirmAndEnable() {
            val ready = _uiState.value.draft as? RecoveryCodeDraftUiState.Ready ?: return
            if (commitInFlight) return
            val owner = pageEffects.currentOwner ?: return
            commitInFlight = true
            replaceDraft(RecoveryCodeDraftUiState.Committing(ready.disclosure))
            viewModelScope.launch {
                try {
                    val result =
                        try {
                            draftInteractor.commit()
                        } catch (cancelled: CancellationException) {
                            reconcileInterruptedCommit(owner, ready.disclosure, restoreReady = true)
                            throw cancelled
                        } catch (error: Error) {
                            reconcileInterruptedCommit(owner, ready.disclosure, restoreReady = false)
                            throw error
                        } catch (_: Exception) {
                            if (withContext(NonCancellable) { draftInteractor.reconcileCommit() }) {
                                RecoveryCodeCommitResult.Committed
                            } else {
                                RecoveryCodeCommitResult.Failed
                            }
                        }
                    when (result) {
                        RecoveryCodeCommitResult.Committed -> {
                            _uiState.update { it.copy(hasRecoveryCode = true) }
                            if (pageEffects.isActive(owner)) {
                                savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                                replaceDraft(RecoveryCodeDraftUiState.Committed)
                            } else {
                                replaceDraft(RecoveryCodeDraftUiState.Empty)
                            }
                        }

                        RecoveryCodeCommitResult.Failed,
                        RecoveryCodeCommitResult.NoActiveDraft,
                        -> {
                            draftInteractor.dismiss()
                            if (pageEffects.isActive(owner)) {
                                savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                                replaceDraft(RecoveryCodeDraftUiState.Failed)
                            } else {
                                replaceDraft(RecoveryCodeDraftUiState.Empty)
                            }
                        }
                    }
                } finally {
                    commitInFlight = false
                }
            }
        }

        private suspend fun reconcileInterruptedCommit(
            owner: Any,
            disclosure: SensitiveValue,
            restoreReady: Boolean,
        ): Boolean {
            val committed = withContext(NonCancellable) { draftInteractor.reconcileCommit() }
            when {
                committed -> {
                    _uiState.update { it.copy(hasRecoveryCode = true) }
                    if (pageEffects.isActive(owner)) {
                        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                        replaceDraft(RecoveryCodeDraftUiState.Committed)
                    } else {
                        replaceDraft(RecoveryCodeDraftUiState.Empty)
                    }
                }

                pageEffects.isActive(owner) && restoreReady -> {
                    replaceDraft(RecoveryCodeDraftUiState.Ready(disclosure))
                }

                else -> {
                    draftInteractor.dismiss()
                    if (pageEffects.isActive(owner)) {
                        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                        replaceDraft(RecoveryCodeDraftUiState.Failed)
                    } else {
                        replaceDraft(RecoveryCodeDraftUiState.Empty)
                    }
                }
            }
            return committed
        }

        private fun dismissDisclosure() {
            if (_uiState.value.draft is RecoveryCodeDraftUiState.Committing) return
            draftInteractor.dismiss()
            savedStateHandle[WAS_DISCLOSURE_OPEN] = false
            replaceDraft(RecoveryCodeDraftUiState.Empty)
        }

        private fun updateVerificationInput(value: String) {
            if (_uiState.value.verification is RecoveryCodeVerificationUiState.Verifying) return
            val replacement = OwnedChars.fromString(value)
            replaceVerification(RecoveryCodeVerificationUiState.Editing(replacement))
        }

        private fun verify() {
            val current = _uiState.value.verification
            val owner = pageEffects.currentOwner ?: return
            if (current is RecoveryCodeVerificationUiState.Verifying || current.input.isEmpty) return
            val input = current.input
            val code = input.toCharArray()
            val request = Any()
            verificationRequest = request
            replaceVerification(RecoveryCodeVerificationUiState.Verifying(input))
            verificationJob =
                viewModelScope.launch {
                    try {
                        val valid = authenticationSettings.verifyRecoveryCode(code)
                        if (pageEffects.isActive(owner) && verificationRequest === request) {
                            replaceVerification(
                                if (valid) {
                                    RecoveryCodeVerificationUiState.Accepted(input)
                                } else {
                                    RecoveryCodeVerificationUiState.Rejected(input)
                                },
                            )
                        }
                    } catch (cancelled: CancellationException) {
                        if (pageEffects.isActive(owner) && verificationRequest === request) {
                            replaceVerification(RecoveryCodeVerificationUiState.Editing(input))
                        }
                        throw cancelled
                    } catch (_: Exception) {
                        if (pageEffects.isActive(owner) && verificationRequest === request) {
                            replaceVerification(RecoveryCodeVerificationUiState.Failed(input))
                        }
                    } finally {
                        code.fill('\u0000')
                        if (verificationRequest === request) {
                            verificationRequest = null
                            verificationJob = null
                        }
                    }
                }
        }

        private fun clearVerificationResult() {
            val current = _uiState.value.verification
            if (current is RecoveryCodeVerificationUiState.Verifying ||
                current is RecoveryCodeVerificationUiState.Editing
            ) {
                return
            }
            replaceVerification(RecoveryCodeVerificationUiState.Editing(current.input))
        }

        private fun replaceVerification(verification: RecoveryCodeVerificationUiState) {
            val previous = _uiState.value.verification.input
            _uiState.update { it.copy(verification = verification) }
            if (previous !== verification.input) previous.wipe()
        }

        private fun clearDisclosureMarker() {
            savedStateHandle.remove<Boolean>(WAS_DISCLOSURE_OPEN)
        }

        private fun replaceDraft(draft: RecoveryCodeDraftUiState) {
            val previous = _uiState.value.draft.disclosureOrNull()
            _uiState.update { it.copy(draft = draft) }
            val current = draft.disclosureOrNull()
            if (previous !== current) previous?.wipe()
        }

        override fun onCleared() {
            _uiState.value.draft
                .disclosureOrNull()
                ?.wipe()
            _uiState.value.verification.input
                .wipe()
            draftInteractor.close()
        }

        private companion object {
            const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
        }
    }

private fun RecoveryCodeDraftUiState.disclosureOrNull() =
    when (this) {
        is RecoveryCodeDraftUiState.Ready -> disclosure
        is RecoveryCodeDraftUiState.Committing -> disclosure
        else -> null
    }

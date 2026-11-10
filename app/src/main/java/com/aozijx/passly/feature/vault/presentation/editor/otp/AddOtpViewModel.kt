package com.aozijx.passly.feature.vault.presentation.editor.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.entry.otp.suggestedEntryTitle
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.port.EntryCreation
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSessionClearReason
import com.aozijx.passly.feature.vault.presentation.editor.toEditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.matchesCurrentAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddOtpViewModel
    @Inject
    internal constructor(
        createEntryUseCase: EntryCreation,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val editEntry: EditEntryUseCase,
    ) : ViewModel() {
        private sealed interface FeedbackScope {
            data object Closed : FeedbackScope

            class Active(
                val authentication: AuthenticationState,
                val pageOwner: Any,
            ) : FeedbackScope
        }

        private val _uiState = MutableStateFlow(AddOtpEditorUiState())
        private var editingEntryId: EntryId? = null
        val uiState = _uiState.asStateFlow()
        private var feedbackScope: FeedbackScope = FeedbackScope.Closed
        private var activePageOwner: Any? = null
        private val _uriAppliedFeedback = Channel<FeedbackScope.Active>(Channel.CONFLATED)
        val uriAppliedFeedback: Flow<Unit> =
            _uriAppliedFeedback
                .receiveAsFlow()
                .filter { owner ->
                    feedbackScope === owner && activePageOwner === owner.pageOwner &&
                        secureSessionAccessState.hasFullSecureSessionAccess() &&
                        secureSessionAccessState.authenticationState.value === owner.authentication
                }.map { Unit }
        private val writeSession =
            EditorWriteSession(
                scope = viewModelScope,
                access = secureSessionAccessState,
                write = { draft ->
                    val entryId = editingEntryId
                    if (entryId == null) {
                        createEntryUseCase(draft)
                    } else {
                        editEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
                    }
                },
                onSaving = { _uiState.value = _uiState.value.copy(isSaving = true) },
                onIdle = {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                },
                onClear = ::clearSensitiveContent,
            )
        internal val effects = writeSession.effects

        fun onPageEntered(): Any =
            Any().also { owner ->
                activePageOwner = owner
                feedbackScope = FeedbackScope.Closed
            }

        fun onPageExited(owner: Any) {
            if (activePageOwner !== owner) return
            activePageOwner = null
            feedbackScope = FeedbackScope.Closed
        }

        fun loadExistingEntry(result: EntryEditorSeedConsumeResult) {
            if (editingEntryId != null) return
            val seed = (result as? EntryEditorSeedConsumeResult.Consumed)?.seed as? EntryEditorSeed.Editable
            if (seed == null || !seed.matchesCurrentAccess(EntryType.OTP, secureSessionAccessState) || !writeSession.canEdit()) {
                failEditLoad()
                return
            }
            editingEntryId = seed.entry.id
            _uiState.value =
                _uiState.value.copy(
                    form = _uiState.value.form.fromEntry(seed.entry),
                    loadPhase = EditorLoadPhase.Ready,
                )
        }

        fun onAction(action: AddOtpAction) {
            when (action) {
                is AddOtpAction.FormChanged -> mutateForm { action.form }
                is AddOtpAction.TypeChanged -> mutateForm { it.withTypeDefaults(action.type) }
                is AddOtpAction.UriChanged -> updateUri(action.value)
                AddOtpAction.Save -> save()
            }
        }

        private fun mutateForm(transform: (OtpFormState) -> OtpFormState): Boolean {
            if (!writeSession.canEdit() || !canEditForm()) return false
            feedbackScope = FeedbackScope.Closed
            val current = _uiState.value
            val form = transform(current.form)
            _uiState.value = current.copy(form = form)
            return true
        }

        private fun save() {
            val current = _uiState.value
            if (!canEditForm()) return
            if (!current.canSave) return
            feedbackScope = FeedbackScope.Closed
            writeSession.save { current.form.toEntryDraft() }
        }

        fun tryApplyScannedConfig(config: OtpConfig): Boolean {
            if (!mutateForm { it.fromParsedConfig(config) }) return false
            publishUriAppliedFeedback()
            return true
        }

        private fun updateUri(value: String) {
            if (!mutateForm { it.copy(uriText = value) } || value.isBlank()) return
            when (val parsed = OtpAuthUriCodec.parse(value)) {
                is OtpUriParseResult.Parsed -> {
                    if (mutateForm { it.fromParsedConfig(parsed.config) }) publishUriAppliedFeedback()
                }

                OtpUriParseResult.NotOtp,
                OtpUriParseResult.Malformed,
                OtpUriParseResult.Unsupported,
                -> Unit
            }
        }

        private fun publishUriAppliedFeedback() {
            val pageOwner = activePageOwner ?: return
            val owner = FeedbackScope.Active(secureSessionAccessState.authenticationState.value, pageOwner)
            feedbackScope = owner
            _uriAppliedFeedback.trySend(owner)
        }

        private fun OtpFormState.withTypeDefaults(type: OtpType): OtpFormState =
            copy(
                type = type,
                digits =
                    when (type) {
                        OtpType.STEAM -> "5"
                        OtpType.TOTP, OtpType.HOTP -> if (digits == "5") "6" else digits
                    },
                period =
                    when (type) {
                        OtpType.STEAM -> "30"
                        OtpType.TOTP -> period.ifBlank { "30" }
                        OtpType.HOTP -> period
                    },
                algorithm = if (type == OtpType.STEAM) OtpHashAlgorithm.SHA1 else algorithm,
                counter = if (type == OtpType.HOTP) counter.ifBlank { "0" } else counter,
            )

        private fun OtpFormState.fromParsedConfig(config: OtpConfig): OtpFormState {
            val parsedAccountName = config.accountName.orEmpty()
            return copy(
                title = config.suggestedEntryTitle(fallback = title),
                accountName = parsedAccountName,
                secret = config.secret.orEmpty(),
                issuer = config.issuer.orEmpty(),
                digits = config.digits.toString(),
                period = (config.periodSeconds ?: 30).toString(),
                counter = (config.counter ?: 0L).toString(),
                algorithm = config.algorithm,
                encoding = config.encoding,
            ).withTypeDefaults(config.type)
        }

        private fun clearSensitiveContent(reason: EditorWriteSessionClearReason) {
            feedbackScope = FeedbackScope.Closed
            _uiState.value =
                AddOtpEditorUiState(
                    loadPhase = reason.toEditorLoadPhase(editingExisting = editingEntryId != null),
                )
        }

        private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

        private fun failEditLoad() {
            feedbackScope = FeedbackScope.Closed
            _uiState.value =
                _uiState.value.copy(
                    form = OtpFormState(),
                    loadPhase = EditorLoadPhase.Failed,
                )
        }

        override fun onCleared() {
            writeSession.close()
            _uriAppliedFeedback.close()
        }
    }

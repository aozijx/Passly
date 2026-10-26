package com.aozijx.passly.feature.vault.presentation.editor.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.suggestedEntryTitle
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private sealed interface FeedbackScope {
        data object Closed : FeedbackScope
        class Active(val authentication: AuthenticationState) : FeedbackScope
    }
    private data class InputEvent(val owner: FeedbackScope.Active, val event: AddOtpEvent)

    private val _uiState = MutableStateFlow(AddOtpCodeState())
    val uiState = _uiState.asStateFlow()
    private var feedbackScope: FeedbackScope = FeedbackScope.Closed
    private val _events = Channel<InputEvent>(Channel.CONFLATED)
    val events: Flow<AddOtpEvent> = _events.receiveAsFlow().filter { delivery ->
        feedbackScope === delivery.owner && secureSessionAccessState.hasFullSecureSessionAccess() &&
            secureSessionAccessState.authenticationState.value === delivery.owner.authentication
    }.map { it.event }
    private val saveSession = EditorSaveSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        create = createEntryUseCase::invoke,
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddOtpSavePhase.Saving) },
        onIdle = {
            _uiState.value = _uiState.value.copy(savePhase = AddOtpSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = saveSession.effects

    fun onAction(action: AddOtpAction) {
        when (action) {
            is AddOtpAction.FormChanged -> mutateForm { action.form }
            is AddOtpAction.TypeChanged -> mutateForm { it.withTypeDefaults(action.type) }
            is AddOtpAction.UriChanged -> updateUri(action.value, action.reportFailure)
            AddOtpAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (OtpFormState) -> OtpFormState): Boolean {
        if (!saveSession.canEdit()) return false
        feedbackScope = FeedbackScope.Closed
        val current = _uiState.value
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
        return true
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        feedbackScope = FeedbackScope.Closed
        saveSession.save { current.form.toEntryDraft() }
    }

    fun tryApplyScannedConfig(config: OtpConfig): Boolean {
        if (!mutateForm { it.fromParsedConfig(config) }) return false
        publishFeedback(AddOtpEvent.UriParsed)
        return true
    }

    private fun updateUri(value: String, reportFailure: Boolean) {
        if (!mutateForm { it.copy(uriText = value) } || value.isBlank()) return
        when (val parsed = OtpAuthUriCodec.parse(value)) {
            is OtpUriParseResult.Parsed -> {
                if (mutateForm { it.fromParsedConfig(parsed.config) }) publishFeedback(AddOtpEvent.UriParsed)
            }
            OtpUriParseResult.NotOtp,
            OtpUriParseResult.Malformed,
            OtpUriParseResult.Unsupported -> if (reportFailure) publishFeedback(AddOtpEvent.UriParseFailed)
        }
    }

    private fun publishFeedback(event: AddOtpEvent) {
        val owner = FeedbackScope.Active(secureSessionAccessState.authenticationState.value)
        feedbackScope = owner
        _events.trySend(InputEvent(owner, event))
    }

    private fun OtpFormState.withTypeDefaults(type: OtpType): OtpFormState = copy(
        type = type,
        digits = when (type) {
            OtpType.STEAM -> "5"
            OtpType.TOTP, OtpType.HOTP -> if (digits == "5") "6" else digits
        },
        period = when (type) {
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

    private fun clearSensitiveContent() {
        feedbackScope = FeedbackScope.Closed
        _uiState.value = AddOtpCodeState()
    }

    override fun onCleared() {
        saveSession.close()
        _events.close()
    }
}

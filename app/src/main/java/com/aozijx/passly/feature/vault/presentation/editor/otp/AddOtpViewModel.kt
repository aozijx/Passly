package com.aozijx.passly.feature.vault.presentation.editor.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    createEntryUseCase: CreateEntryUseCase,
    secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddOtpCodeState())
    val uiState = _uiState.asStateFlow()
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
    private val _events = Channel<AddOtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: AddOtpAction) {
        when (action) {
            is AddOtpAction.FormChanged -> mutateForm { action.form }
            is AddOtpAction.TypeChanged -> mutateForm { it.withTypeDefaults(action.type) }
            is AddOtpAction.UriChanged -> updateUri(action.value, action.reportFailure)
            is AddOtpAction.ScannedConfigApplied -> applyScannedConfig(action.config)
            AddOtpAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (OtpFormState) -> OtpFormState) {
        if (!saveSession.canEdit()) return
        val current = _uiState.value
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        saveSession.save { current.form.toEntryDraft() }
    }

    private fun applyScannedConfig(config: OtpConfig) {
        if (!saveSession.canEdit()) return
        mutateForm { it.fromParsedConfig(config) }
        _events.trySend(AddOtpEvent.UriParsed)
    }

    private fun updateUri(value: String, reportFailure: Boolean) {
        if (!saveSession.canEdit()) return
        mutateForm { it.copy(uriText = value) }
        if (_uiState.value.savePhase == AddOtpSavePhase.Saving || value.isBlank()) return
        when (val parsed = OtpAuthUriCodec.parse(value)) {
            is OtpUriParseResult.Parsed -> {
                mutateForm { it.fromParsedConfig(parsed.config) }
                _events.trySend(AddOtpEvent.UriParsed)
            }
            OtpUriParseResult.NotOtp,
            OtpUriParseResult.Malformed,
            OtpUriParseResult.Unsupported -> if (reportFailure) _events.trySend(AddOtpEvent.UriParseFailed)
        }
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
        val parsedTitle = listOfNotNull(
            config.issuer?.takeIf(String::isNotBlank),
            parsedAccountName.takeIf(String::isNotBlank),
        ).joinToString(": ")
        return copy(
            title = parsedTitle.ifBlank { title },
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
        _uiState.value = AddOtpCodeState()
    }

    override fun onCleared() {
        saveSession.close()
    }
}

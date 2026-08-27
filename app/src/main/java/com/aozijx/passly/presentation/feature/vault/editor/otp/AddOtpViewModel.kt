package com.aozijx.passly.presentation.feature.vault.editor.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.vault.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddOtpCodeState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<AddOtpEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val _events = Channel<AddOtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            secureSessionAccessState.authenticationState.collect {
                if (!secureSessionAccessState.hasFullSecureSessionAccess()) clearSensitiveContent()
            }
        }
    }

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
        val current = _uiState.value
        if (current.isSaving) return
        val form = transform(current.form)
        _uiState.value = current.copy(form = form, canSave = form.isValid)
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        _uiState.value = current.copy(canSave = false, isSaving = true)
        viewModelScope.launch {
            try {
                when (val result = createEntryUseCase(current.form.toEntryDraft())) {
                    is AppResult.Success -> {
                        clearSensitiveContent()
                        _effects.send(AddOtpEffect.Saved)
                    }
                    is AppResult.Failure -> restoreAfterFailure(result.error.code)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                restoreAfterFailure("创建条目失败")
            }
        }
    }

    private suspend fun restoreAfterFailure(message: String?) {
        val form = _uiState.value.form
        _uiState.value = _uiState.value.copy(canSave = form.isValid, isSaving = false)
        _effects.send(AddOtpEffect.SaveFailed(message))
    }

    private fun applyScannedConfig(config: OtpConfig) {
        mutateForm { it.fromParsedConfig(config) }
        _events.trySend(AddOtpEvent.UriParsed)
    }

    private fun updateUri(value: String, reportFailure: Boolean) {
        mutateForm { it.copy(uriText = value) }
        if (_uiState.value.isSaving || value.isBlank()) return
        val parsed = OtpAuthUriCodec.parse(value)
        if (parsed == null) {
            if (reportFailure) _events.trySend(AddOtpEvent.UriParseFailed)
            return
        }
        mutateForm { it.fromParsedConfig(parsed) }
        _events.trySend(AddOtpEvent.UriParsed)
    }

    private fun OtpFormState.withTypeDefaults(type: OtpType): OtpFormState = copy(
        type = type,
        digits = when (type) {
            OtpType.STEAM -> "5"
            OtpType.TOTP -> if (digits == "5") "6" else digits
            OtpType.HOTP -> digits
        },
        period = when (type) {
            OtpType.STEAM -> "30"
            OtpType.TOTP -> period.ifBlank { "30" }
            OtpType.HOTP -> period
        },
        algorithm = if (type == OtpType.STEAM) "SHA1" else algorithm,
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
            algorithm = config.algorithm.name,
            encoding = config.encoding,
        ).withTypeDefaults(config.type)
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddOtpCodeState()
    }

    override fun onCleared() {
        clearSensitiveContent()
    }
}

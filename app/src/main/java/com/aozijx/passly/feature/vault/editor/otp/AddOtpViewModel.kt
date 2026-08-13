package com.aozijx.passly.feature.vault.editor.otp

import com.aozijx.passly.core.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.editor.common.CreateEntryViewModel
import com.aozijx.passly.feature.vault.model.OtpFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    entryCommandRepository: EntryCommandRepository,
    secureSessionAccessState: SecureSessionAccessState
) : CreateEntryViewModel<OtpFormState>(
    initialForm = OtpFormState(),
    entryCommandRepository = entryCommandRepository,
    secureSessionAccessState = secureSessionAccessState,
    isFormValid = OtpFormState::isValid,
    createEntry = { OtpEntryFactory.create(it) }
) {

    private val _events = Channel<AddOtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: AddOtpAction) {
        when (action) {
            is AddOtpAction.FormChanged -> mutateForm { action.form }
            is AddOtpAction.TypeChanged -> mutateForm { it.withTypeDefaults(action.type) }
            is AddOtpAction.UriChanged -> updateUri(action.value, action.reportFailure)
            is AddOtpAction.ScannedConfigApplied -> applyScannedConfig(action.config)
            AddOtpAction.Save -> saveEntry()
        }
    }

    private fun applyScannedConfig(config: OtpConfig) {
        mutateForm { it.fromParsedConfig(config) }
        _events.trySend(AddOtpEvent.UriParsed)
    }

    private fun updateUri(value: String, reportFailure: Boolean) {
        mutateForm { it.copy(uriText = value) }
        if (uiState.value.isSaving || value.isBlank()) return

        val parsed = OtpAuthUriCodec.parse(value)
        if (parsed == null) {
            if (reportFailure) {
                _events.trySend(AddOtpEvent.UriParseFailed)
            }
            return
        }

        mutateForm {
            it.fromParsedConfig(parsed)
        }
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
        counter = if (type == OtpType.HOTP) counter.ifBlank { "0" } else counter
    )

    private fun OtpFormState.fromParsedConfig(config: OtpConfig): OtpFormState {
        val accountName = config.accountName.orEmpty()
        val parsedTitle = listOfNotNull(
            config.issuer?.takeIf(String::isNotBlank),
            accountName.takeIf(String::isNotBlank)
        ).joinToString(": ")

        return copy(
            title = parsedTitle.ifBlank { title },
            username = accountName,
            secret = config.secret,
            issuer = config.issuer.orEmpty(),
            domain = config.issuer.orEmpty(),
            digits = config.digits.toString(),
            period = (config.periodSeconds ?: 30).toString(),
            counter = (config.counter ?: 0L).toString(),
            algorithm = config.algorithm.name,
            encoding = config.encoding
        ).withTypeDefaults(config.type)
    }
}

package com.aozijx.passly.feature.vault.editor.otp

import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.feature.vault.editor.common.CreateEntryViewModel
import com.aozijx.passly.feature.vault.model.OtpFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent
    data object UriParseFailed : AddOtpEvent
}

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    entryCommandRepository: EntryCommandRepository
) : CreateEntryViewModel<OtpFormState>(
    initialForm = OtpFormState(),
    entryCommandRepository = entryCommandRepository,
    isFormValid = OtpFormState::isValid,
    createEntry = { OtpEntryFactory.create(it) }
) {

    private val _events = Channel<AddOtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun updateForm(form: OtpFormState) = mutateForm { form }

    fun updateType(type: OtpType) = mutateForm {
        it.withTypeDefaults(type)
    }

    fun updateUri(value: String, reportFailure: Boolean = false) {
        mutateForm { it.copy(uriText = value) }
        if (uiState.value.isSaving || value.isBlank()) return

        val parsed = TotpUtils.parseOtpAuthUri(value)
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

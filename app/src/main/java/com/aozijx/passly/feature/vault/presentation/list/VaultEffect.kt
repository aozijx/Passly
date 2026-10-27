package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface VaultEffect {
    data class ShowError(val message: UiErrorMessage) : VaultEffect
    data class FieldCopied(val fieldKey: FieldKey) : VaultEffect
    data object OtpCopied : VaultEffect
}

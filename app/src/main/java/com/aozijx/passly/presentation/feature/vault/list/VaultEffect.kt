package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.FieldKey

sealed interface VaultEffect {
    data class ShowToast(val message: String) : VaultEffect
    data class ShowError(val message: String) : VaultEffect
    data class FieldCopied(val fieldKey: FieldKey) : VaultEffect
    data object OtpCopied : VaultEffect
}

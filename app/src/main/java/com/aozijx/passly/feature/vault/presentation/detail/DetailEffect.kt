package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey

sealed interface DetailEffect {
    data class ContentCopied(val fieldKey: FieldKey) : DetailEffect
    data object OtpCopied : DetailEffect
}

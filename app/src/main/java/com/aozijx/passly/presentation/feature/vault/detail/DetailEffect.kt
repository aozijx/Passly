package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey

sealed interface DetailEffect {
    data class ShowOtpQr(val uri: String) : DetailEffect
    data class ContentCopied(val fieldKey: FieldKey?) : DetailEffect
}

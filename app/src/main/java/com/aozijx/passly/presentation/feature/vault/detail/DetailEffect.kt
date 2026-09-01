package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry

sealed interface DetailEffect {
    data class EntryUpdated(val entry: Entry) : DetailEffect
    data class ShowOtpQr(val uri: String) : DetailEffect
}

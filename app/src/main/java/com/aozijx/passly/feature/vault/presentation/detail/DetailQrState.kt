package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.detail.EntryQrKind

sealed interface DetailQrState {
    data object Hidden : DetailQrState
    data class Loading(val kind: EntryQrKind) : DetailQrState
    data class Ready(val kind: EntryQrKind, val content: DetailQrPayload) : DetailQrState
    data class Failed(val kind: EntryQrKind) : DetailQrState
}

package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.qr.QrCodeMatrix

enum class EntryQrKind { Otp, Wifi }

sealed interface DetailQrState {
    data object Hidden : DetailQrState

    data class Authorizing(
        val kind: EntryQrKind,
    ) : DetailQrState

    data class Loading(
        val kind: EntryQrKind,
    ) : DetailQrState

    data class Ready(
        val kind: EntryQrKind,
        val matrix: QrCodeMatrix,
    ) : DetailQrState

    data class Failed(
        val kind: EntryQrKind,
    ) : DetailQrState
}

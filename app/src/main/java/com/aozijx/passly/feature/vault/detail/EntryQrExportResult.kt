package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.sensitive.OwnedChars

enum class EntryQrKind { Otp, Wifi }

internal sealed interface EntryQrExportResult {
    data class Ready(val kind: EntryQrKind, val content: OwnedChars) : EntryQrExportResult
    data object Unavailable : EntryQrExportResult
    data object Denied : EntryQrExportResult
    data object Cancelled : EntryQrExportResult
}

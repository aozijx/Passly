package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.sensitive.OwnedChars

internal sealed interface EntryQrExportResult {
    data class Ready(
        val content: OwnedChars,
    ) : EntryQrExportResult

    data object Unavailable : EntryQrExportResult

    data object Denied : EntryQrExportResult

    data object Cancelled : EntryQrExportResult
}

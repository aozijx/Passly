package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType

internal val Entry.detailDisplayTitle: String
    get() = if (type == EntryType.WIFI) {
        secret.wifi?.ssid?.takeIf(String::isNotBlank) ?: title
    } else {
        title
    }

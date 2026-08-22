package com.aozijx.passly.feature.detail.contract

import com.aozijx.passly.domain.entry.model.Entry

sealed interface DetailEffect {
    data class EntryUpdated(val entry: Entry) : DetailEffect
}

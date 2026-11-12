package com.aozijx.passly.data.local.database.model

import com.aozijx.passly.domain.entry.model.EntryType

internal data class EntryTypeProjection(
    val entryId: String,
    val entryType: EntryType,
)

package com.aozijx.passly.domain.model.revision

import com.aozijx.passly.domain.model.entry.VaultEntry

data class EntrySnapshot(
    val snapshotId: String,
    val entry: VaultEntry
)

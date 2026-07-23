package com.aozijx.passly.domain.model.snapshot

import com.aozijx.passly.domain.model.entry.VaultEntry

data class EntrySnapshot(
    val snapshotId: String,
    val entry: VaultEntry
)

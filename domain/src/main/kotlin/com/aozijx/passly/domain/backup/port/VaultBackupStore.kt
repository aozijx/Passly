package com.aozijx.passly.domain.backup.port

import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreResult
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot

/** Reads and restores vault data without exposing persistence or archive-format types. */
interface VaultBackupStore {
    suspend fun read(options: VaultBackupReadOptions): VaultBackupSnapshot

    suspend fun restore(
        snapshot: VaultBackupSnapshot,
        mode: VaultBackupRestoreMode,
    ): VaultBackupRestoreResult
}

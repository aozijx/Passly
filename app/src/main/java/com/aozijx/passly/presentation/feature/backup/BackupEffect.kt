package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal sealed interface BackupEffect {
    val operation: BackupOperation

    data class Succeeded(
        override val operation: BackupOperation,
    ) : BackupEffect

    data class Failed(
        override val operation: BackupOperation,
    ) : BackupEffect
}

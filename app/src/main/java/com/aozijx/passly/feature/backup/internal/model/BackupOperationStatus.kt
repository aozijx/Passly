package com.aozijx.passly.feature.backup.internal.model

sealed interface BackupOperationStatus {
    data object Idle : BackupOperationStatus
    data object Loading : BackupOperationStatus
    data object Failure : BackupOperationStatus
}
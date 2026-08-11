package com.aozijx.passly.domain.backup.model

sealed interface BackupOperationStatus {
    data object Idle : BackupOperationStatus
    data object Loading : BackupOperationStatus
    data object Failure : BackupOperationStatus
}
package com.aozijx.passly.feature.backup.contract

import com.aozijx.passly.core.error.AppError

sealed interface BackupEffect {
    data class ShowError(val error: AppError) : BackupEffect
}

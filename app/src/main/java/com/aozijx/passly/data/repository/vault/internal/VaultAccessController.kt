package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.security.crypto.VaultLockManager

internal fun <T> VaultLockManager.failIfLocked(message: String = "数据库未解锁"): AppResult<T>? {
    return if (isLocked()) {
        @Suppress("UNCHECKED_CAST")
        AppResult.failure(AppError.AuthFailed(message)) as AppResult<T>
    } else {
        null
    }
}

internal inline fun <T> VaultLockManager.ifLockedReturn(block: () -> T): T? {
    return if (isLocked()) block() else null
}

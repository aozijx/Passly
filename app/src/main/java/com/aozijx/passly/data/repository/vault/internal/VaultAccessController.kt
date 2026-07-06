package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.security.crypto.VaultLockManager

internal fun VaultLockManager.requireUnlocked() {
    if (isLocked()) {
        throw VaultLockedException("Vault is locked, authentication required")
    }
}

internal fun VaultLockManager.throwIfLocked() = requireUnlocked()

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

internal inline fun <T> VaultLockManager.unlocked(block: () -> T): T? {
    return if (isLocked()) null else block()
}

class VaultLockedException(message: String) : IllegalStateException(message)

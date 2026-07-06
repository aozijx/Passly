package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.security.crypto.VaultLockManager

internal inline fun <T> VaultLockManager.withLockGuard(
    onLocked: () -> T,
    block: () -> T
): T {
    return if (isLocked()) onLocked() else block()
}
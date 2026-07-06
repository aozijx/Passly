package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.core.crypto.keystore.BiometricPassphraseBridge

internal inline fun <T> BiometricPassphraseBridge.withLockGuard(
    onLocked: () -> T,
    block: () -> T
): T {
    return if (isLocked) onLocked() else block()
}
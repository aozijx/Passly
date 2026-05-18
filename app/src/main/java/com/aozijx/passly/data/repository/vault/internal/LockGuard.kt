package com.aozijx.passly.data.repository.vault.internal

import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager

internal inline fun <T> withLockGuard(onLocked: () -> T, block: () -> T): T {
    return if (DatabasePassphraseManager.isLocked) onLocked() else block()
}
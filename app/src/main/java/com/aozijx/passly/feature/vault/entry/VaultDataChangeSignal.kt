package com.aozijx.passly.feature.vault.entry

import kotlinx.coroutines.flow.Flow

fun interface VaultDataChangeSignal {
    fun changes(): Flow<Unit>
}

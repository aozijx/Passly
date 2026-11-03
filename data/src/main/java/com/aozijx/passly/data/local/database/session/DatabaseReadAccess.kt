package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class DatabaseReadAccess(
    val readable: Boolean,
    val lockGeneration: Long,
)

internal fun observeDatabaseReadAccess(
    isAuthorized: Flow<Boolean>,
    databaseLockState: Flow<SecureSessionState>,
    lockGeneration: Flow<Long>,
): Flow<DatabaseReadAccess> =
    combine(isAuthorized, databaseLockState, lockGeneration) { authorized, state, generation ->
        val readable = authorized && state == SecureSessionState.UNLOCKED
        DatabaseReadAccess(
            readable = readable,
            lockGeneration = if (readable) generation else 0L,
        )
    }.distinctUntilChanged()

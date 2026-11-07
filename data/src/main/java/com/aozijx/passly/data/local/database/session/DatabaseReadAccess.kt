package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class DatabaseReadAccess(
    val readable: Boolean,
    val lockGeneration: Long,
)

internal fun observeDatabaseReadAccess(
    isAuthorized: Flow<Boolean>,
    databaseLockState: Flow<SecureSessionState>,
    lockGeneration: StateFlow<Long>,
): Flow<DatabaseReadAccess> =
    combine(isAuthorized, databaseLockState, lockGeneration) { authorized, state, _ ->
        val readable = authorized && state == SecureSessionState.UNLOCKED
        // Lock state and generation publish separately; sample the latest generation so a fast reopen
        // cannot briefly create a second readable projection and restart the Room subscription twice.
        DatabaseReadAccess(
            readable = readable,
            lockGeneration = if (readable) lockGeneration.value else 0L,
        )
    }.distinctUntilChanged()

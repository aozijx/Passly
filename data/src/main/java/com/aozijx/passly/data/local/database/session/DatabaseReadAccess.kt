package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal fun observeDatabaseReadAccess(
    isAuthorized: Flow<Boolean>,
    databaseLockState: Flow<SecureSessionState>,
): Flow<Boolean> = combine(isAuthorized, databaseLockState) { authorized, state ->
    authorized && state == SecureSessionState.UNLOCKED
}.distinctUntilChanged()

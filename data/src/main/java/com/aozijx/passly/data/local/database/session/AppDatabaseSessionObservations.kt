package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SessionLockedException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> observeDatabaseFlowWhenReadable(
    readAccess: Flow<DatabaseReadAccess>,
    emptyValue: () -> T,
    query: suspend () -> Flow<T>,
): Flow<T> =
    readAccess.flatMapLatest { access ->
        if (access.readable) {
            query().catch { failure ->
                if (failure is SessionLockedException) emit(emptyValue()) else throw failure
            }
        } else {
            flowOf(emptyValue())
        }
    }

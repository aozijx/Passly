package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.data.local.database.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Recreates a Room observation only while both authorization and the database session allow reads. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> AppDatabaseSession.observeWhenReadable(
    authorized: Flow<Boolean>,
    emptyValue: () -> T,
    query: suspend AppDatabase.() -> Flow<T>,
): Flow<T> = observeDatabaseReadAccess(authorized, lockStateFlow).flatMapLatest { readable ->
    if (readable) observeFlow(query) else flowOf(emptyValue())
}

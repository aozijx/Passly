package com.aozijx.passly.data.local.database.session

import androidx.room.withTransaction
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.runtime.session.SessionResource
import javax.inject.Inject

internal class AppDatabaseSessionResource @Inject constructor(
    private val databaseProvider: DatabaseProvider,
) : SessionResource<AppDatabase> {
    override suspend fun open(key: ByteArray): AppDatabase = databaseProvider.open(key)

    override suspend fun close(handle: AppDatabase) = handle.close()

    override suspend fun <T> transaction(
        handle: AppDatabase,
        block: suspend AppDatabase.() -> T,
    ): T = handle.withTransaction { handle.block() }
}

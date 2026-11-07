package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.activity.toDomain
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomActivityQueryRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
    ) : ActivityQueryRepository {
        override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { emptyList() }) {
                entryActivityQueryDao()
                    .observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
    }

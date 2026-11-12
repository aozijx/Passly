package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询 Repository 实现：仅执行读操作，不涉及事务。
 * 直接对接 DAO 查询，无写操作副作用。
 */
@Singleton
internal class RoomEntryQueryRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
        private val secretFieldStore: SecretFieldStore,
    ) : EntryQueryRepository,
        EntryTagQuery {
        override suspend fun getTypesByIds(entryIds: List<EntryId>): Map<EntryId, EntryType> {
            if (entryIds.isEmpty()) return emptyMap()
            return databaseSession.queryFullVault(
                onDenied = { emptyMap() },
            ) {
                entryIds
                    .distinct()
                    .chunked(MAX_ENTRY_IDS_PER_QUERY)
                    .flatMap { ids -> entryQueryDao().getTypesByIds(ids.map(EntryId::value)) }
                    .associate { EntryId(it.entryId) to it.entryType }
            }
        }

        override suspend fun getById(entryId: EntryId): Entry? {
            return databaseSession.queryFullVault(
                onDenied = { null },
            ) {
                val metaEntity = entryQueryDao().getById(entryId.value) ?: return@queryFullVault null
                val summary = EntryProfileMapper.fromEntity(metaEntity)
                val secret = secretFieldStore.readBundle(this, entryId.value, metaEntity.entryType)
                EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)
            }
        }

        override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> {
            if (entryIds.isEmpty()) return emptyList()
            return databaseSession.queryFullVault(
                onDenied = { emptyList() },
            ) {
                val requestedIds = entryIds.distinct()
                val entriesById =
                    requestedIds
                        .chunked(MAX_ENTRY_IDS_PER_QUERY)
                        .flatMap { ids -> entryQueryDao().getByIds(ids.map(EntryId::value)) }
                        .associateBy { it.entryId }
                requestedIds.mapNotNull { id ->
                    val metaEntity = entriesById[id.value] ?: return@mapNotNull null
                    val summary = EntryProfileMapper.fromEntity(metaEntity)
                    val secret = secretFieldStore.readBundle(this, id.value, metaEntity.entryType)
                    EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)
                }
            }
        }

        override suspend fun findAllTags(): Set<String> =
            databaseSession.queryFullVault(
                onDenied = { emptySet() },
            ) {
                entryQueryDao().getActive().flatMapTo(linkedSetOf()) { it.tags }
            }

        private companion object {
            const val MAX_ENTRY_IDS_PER_QUERY = 900
        }
    }

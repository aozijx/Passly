package com.aozijx.passly.data.repository.command

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.lookup.toLookupFields
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.entity.VaultSnapshotEntity
import com.aozijx.passly.data.repository.VaultUnitOfWork
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.history.SnapshotType
import com.aozijx.passly.security.search.BlindIndexer
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 业务语义的 Command Handler。
 *
 * 替代原有的 CommandRepository + VaultCommandUseCases + DetailCommandUseCases
 * + IconResyncUseCases + RecordEntryUsageFacade。
 *
 * 每条命令：
 * - 通过 [VaultUnitOfWork] 访问数据库，不直接引用 DAO 或 Room
 * - 使用 SQL 级乐观锁（optimisticUpdate / optimisticSoftDelete）
 * - 原子写入 Metadata + Credential + 版本 + 盲索引 + 历史快照 + 活动记录
 */
@Singleton
class EntryCommandHandler @Inject constructor(
    private val unitOfWork: VaultUnitOfWork,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val blindIndexer: BlindIndexer,
    private val clock: Clock
) {

    // =========================== 创建 ===========================

    /**
     * 创建新条目。
     * 原子写入：Metadata + Credential + 盲索引 + 历史快照 + 活动记录。
     */
    suspend fun createEntry(entry: VaultEntry): AppResult<Long> = unitOfWork.write("entry.create") {
        val now = clock.now()
        val entryId = entry.id.ifEmpty { UuidCreator.getTimeOrderedEpoch().toString() }

        val metaBlob = cryptoMapper.encryptMetadata(entry.metadata, entryId)
        val credBlob = cryptoMapper.encryptCredential(entry.credential, entryId)

        val metaEntity = VaultMetadataEntity(
            entryId = entryId,
            entryType = entry.entryType,
            metadataBlob = metaBlob,
            createdAt = now,
            updatedAt = now
        )
        val credEntity = VaultCredentialEntity(
            entryId = entryId,
            credentialBlob = credBlob
        )

        metadataDao().insert(metaEntity)
        credentialDao().insert(credEntity)

        // 盲索引
        val indexedEntry = entry.copy(metadata = entry.metadata.copy(entryId = entryId))
        val indexRecords = blindIndexer.index(entryId, indexedEntry.toLookupFields())
        if (indexRecords.isNotEmpty()) {
            lookupIndexDao().insertAll(indexRecords.toEntityList())
        }

        // 历史快照
        historyDao().insert(
            VaultSnapshotEntity(
                version = 1,
                entryId = entryId,
                snapshotBlob = metaBlob + credBlob,
                changeType = SnapshotType.VALUE_CHANGED.value,
                createdAt = now
            )
        )

        // 活动记录
        activityDao().insert(
            VaultActivity(entryId = entryId, activityType = ActivityType.CREATE).toEntity(now)
        )

        0L
    }

    // =========================== 通用更新 ===========================

    /**
     * 更新条目的 Metadata 和/或 Credential。
     *
     * 一次事务原子写入：Metadata(含版本) + Credential + 盲索引 + 历史快照。
     * 覆盖所有字段（title, username, password, email, notes, otp, card,
     * ssh, seedPhrase, customFields 等），替代原有的多个单字段命令。
     */
    suspend fun updateEntry(
        id: String, expectedVersion: Int, changes: EntryChanges
    ): AppResult<Unit> = unitOfWork.write("entry.update") {
        val metaEntity = metadataDao().getById(id) ?: return@write
        val oldMeta = cryptoMapper.decryptMetadata(metaEntity)
        val credEntity = credentialDao().getByEntryId(id)
        val oldCred = credEntity?.let { cryptoMapper.decryptCredential(it) }

        val newMeta = changes.metadata ?: oldMeta
        val newCred = changes.credential ?: (oldCred ?: VaultCredential(entryId = id))
        val now = clock.now()

        // 1. 版本校验 + metadata 更新（原子操作）
        val metaBlob = cryptoMapper.encryptMetadata(newMeta, id)
        val affected = metadataDao().optimisticUpdate(id, expectedVersion, metaBlob, now)
        unitOfWork.checkAffectedRows(id, expectedVersion, affected)

        // 2. 写入 Credential（有变更或首次创建凭据时更新）
        if (changes.credential != null || oldCred == null) {
            val credBlob = cryptoMapper.encryptCredential(newCred, id)
            credentialDao().updateBlob(id, credBlob)
        }

        // 3. 重建盲索引（仅搜索相关字段变化时）
        val searchFieldsChanged =
            oldMeta.title != newMeta.title || oldMeta.username != newMeta.username
        if (changes.metadata != null && searchFieldsChanged) {
            rebuildBlindIndex(id, metaEntity, newMeta)
        }

        // 4. 历史快照
        snapshotChanges(
            id,
            metaEntity,
            oldMeta,
            if (changes.credential != null) newCred else null,
            now
        )
    }

    // =========================== 生命周期 ===========================

    /**
     * 移入回收站（软删除）。
     * 原子写入：Metadata 软删除 + 盲索引清理 + 历史快照 + 活动记录。
     */
    suspend fun moveToTrash(
        id: String, expectedVersion: Int
    ): AppResult<Unit> = unitOfWork.write("entry.moveToTrash") {
        val now = clock.now()
        val affected = metadataDao().optimisticSoftDelete(id, expectedVersion, now, now)
        unitOfWork.checkAffectedRows(id, expectedVersion, affected)

        lookupIndexDao().deleteByEntryId(id)

        // 活动记录
        activityDao().insert(
            VaultActivity(entryId = id, activityType = ActivityType.DELETE).toEntity(now)
        )
    }

    /**
     * 恢复回收站中的条目。
     */
    suspend fun restoreEntry(
        id: String
    ): AppResult<Unit> = unitOfWork.write("entry.restore") {
        val now = clock.now()
        metadataDao().restore(id, now)

        // 重建盲索引
        val metaEntity = metadataDao().getById(id) ?: return@write
        val meta = cryptoMapper.decryptMetadata(metaEntity)
        rebuildBlindIndex(id, metaEntity, meta)
    }

    // =========================== 搜索索引 ===========================

    /**
     * 重建所有条目的盲索引。
     * 用于首次解锁后的存量数据迁移。
     */
    suspend fun rebuildIndex(): AppResult<Int> = unitOfWork.write("entry.rebuildIndex") {
        val metaEntities = metadataDao().getActive()
        if (metaEntities.isEmpty()) return@write 0

        val entryIds = metaEntities.map { it.entryId }
        val credEntities = credentialDao().getByEntryIds(entryIds)
        val credMap = credEntities.associateBy { it.entryId }

        lookupIndexDao().clear()

        var indexedCount = 0
        for (metaEntity in metaEntities) {
            val credEntity = credMap[metaEntity.entryId]
            val entry = cryptoMapper.assembleEntry(metaEntity, credEntity) ?: continue
            val indexRecords = blindIndexer.index(entry.id, entry.toLookupFields())
            if (indexRecords.isNotEmpty()) {
                lookupIndexDao().insertAll(indexRecords.toEntityList())
                indexedCount++
            }
        }

        indexedCount
    }

    // =========================== 使用记录 ===========================

    /**
     * 记录条目使用事件。
     * 原子写入：活动记录 + 更新 usageCount / lastUsedAt。
     */
    suspend fun recordUsage(
        entryId: String, type: ActivityType = ActivityType.VIEW
    ): AppResult<Unit> = unitOfWork.write("entry.recordUsage") {
        val now = clock.now()

        // 1. 插入活动记录
        activityDao().insert(
            VaultActivity(entryId = entryId, activityType = type).toEntity(now)
        )

        // 2. 更新元数据中的使用统计
        val metaEntity = metadataDao().getById(entryId)
            ?: return@write
        val meta = cryptoMapper.decryptMetadata(metaEntity)
        val updatedMeta = meta.copy(
            usageCount = meta.usageCount + 1,
            lastUsedAt = now
        )

        val metaBlob = cryptoMapper.encryptMetadata(updatedMeta, entryId)
        val expectedVersion = metaEntity.entryVersion
        val affected = metadataDao().optimisticUpdate(entryId, expectedVersion, metaBlob, now)
        if (affected == 0) {
            // usageCount 更新不阻塞业务，版本冲突时静默跳过
        }
    }

    // =========================== 内部方法 ===========================

    /**
     * 重建指定条目的盲索引。
     */
    private suspend fun AppDatabase.rebuildBlindIndex(
        id: String,
        metaEntity: VaultMetadataEntity,
        meta: VaultMetadata
    ) {
        val entry = VaultEntry(meta, VaultCredential(entryId = id)).copy(
            metadata = meta.copy(entryId = id)
        )
        lookupIndexDao().deleteByEntryId(id)
        val indexRecords = blindIndexer.index(id, entry.toLookupFields())
        if (indexRecords.isNotEmpty()) {
            lookupIndexDao().insertAll(indexRecords.toEntityList())
        }
    }

    /**
     * 写入历史快照。
     */
    private suspend fun AppDatabase.snapshotChanges(
        id: String,
        oldMetaEntity: VaultMetadataEntity,
        meta: VaultMetadata,
        cred: VaultCredential?,
        now: Long
    ) {
        val newVersion = oldMetaEntity.entryVersion + 1
        val metaBlob = cryptoMapper.encryptMetadata(meta, id)
        val credBlob = if (cred != null) cryptoMapper.encryptCredential(cred, id)
        else credentialDao().getByEntryId(id)?.credentialBlob ?: ByteArray(0)

        historyDao().insert(
            VaultSnapshotEntity(
                version = newVersion,
                entryId = id,
                snapshotBlob = metaBlob + credBlob,
                changeType = SnapshotType.VALUE_CHANGED.value,
                createdAt = now
            )
        )
    }

    private companion object {

        private fun List<com.aozijx.passly.security.search.BlindIndexRecord>.toEntityList(): List<LookupIndexEntity> =
            map { record ->
                LookupIndexEntity(
                    entryId = record.entryId,
                    field = record.field,
                    keywordHash = record.keywordHash,
                    gramLength = record.gramLength,
                    weight = record.weight
                )
            }

        private fun VaultActivity.toEntity(now: Long) =
            com.aozijx.passly.data.model.entity.VaultActivityEntity(
                entryId = entryId,
                activityType = activityType,
                createdAt = now
            )
    }
}

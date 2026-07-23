package com.aozijx.passly.data.repository.command

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.NotFound
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.data.local.dao.scanIndexStatus
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.data.repository.VaultUnitOfWork
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.revision.RevisionType
import com.aozijx.passly.domain.repository.entry.EntryCommands
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
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val revisionCodec: EntryRevisionCodec,
    private val blindIndexer: BlindIndexer,
    private val clock: Clock
) : EntryCommands {

    // =========================== 创建 ===========================

    /**
     * 创建新条目。
     * 原子写入：Metadata + Credential + 盲索引 + 历史快照 + 活动记录。
     */
    override suspend fun createEntry(entry: VaultEntry): AppResult<Long> =
        unitOfWork.write("entry.create") {
        val now = clock.now()
        val entryId = entry.id.ifEmpty { UuidCreator.getTimeOrderedEpoch().toString() }

            val metaBlob = summaryCodec.encrypt(entry.summary, entryId)
            val credBlob = secretCodec.encrypt(entry.secret, entryId)

            val metaEntity = EntryEntity(
            entryId = entryId,
            entryType = entry.entryType,
                summaryBlob = metaBlob,
            createdAt = now,
            updatedAt = now
        )
            val credEntity = EntrySecretEntity(
            entryId = entryId,
                secretBlob = credBlob
        )

            entryDao().insertStrict(metaEntity)
            entrySecretDao().insertStrict(credEntity)

        // 盲索引
            val indexRecords = blindIndexer.index(entryId, entry.toLookupFields())
        if (indexRecords.isNotEmpty()) {
            searchTokenDao().upsertAllForImport(indexRecords.toEntityList())
        }

            // 历史快照（使用统一 SnapshotPayload 格式）
            entryRevisionDao().insertStrict(
                EntryRevisionEntity(
                    revisionId = UuidCreator.getTimeOrderedEpoch().toString(),
                version = 1,
                entryId = entryId,
                    snapshotBlob = revisionCodec.encrypt(
                        entry.summary,
                        entry.secret,
                    entryId
                ),
                    changeType = RevisionType.VALUE_CHANGED.value,
                createdAt = now
            )
        )

        // 活动记录
            entryActivityDao().insertStrict(
                EntryActivity(entryId = entryId, activityType = ActivityType.CREATE).toEntity(now)
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
    override suspend fun updateEntry(
        id: String, expectedVersion: Int, changes: EntryChanges
    ): AppResult<Unit> = unitOfWork.write("entry.update") {
        val metaEntity = entryDao().getById(id)
            ?: throw NotFound("entry:$id not found")
        val oldSummary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
        val credEntity = entrySecretDao().getByEntryId(id)
        val oldSecret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }

        val newSummary = changes.summary ?: oldSummary
        val newSecret = changes.secret ?: (oldSecret ?: EntrySecret.VaultData())
        val now = clock.now()

        // 1. 版本校验 + metadata 更新（原子操作）
        val metaBlob = summaryCodec.encrypt(newSummary, id)
        val affected = entryDao().optimisticUpdate(id, expectedVersion, metaBlob, now)
        unitOfWork.checkAffectedRows(id, expectedVersion, affected)

        // 2. 写入 Secret（有变更或首次创建凭据时更新）
        if (changes.secret != null || oldSecret == null) {
            val credBlob = secretCodec.encrypt(newSecret, id)
            entrySecretDao().updateBlob(id, credBlob)
        }

        // 3. 重建盲索引（仅搜索相关字段变化时）
        val searchFieldsChanged =
            oldSummary.title != newSummary.title || oldSummary.username != newSummary.username
        if (changes.summary != null && searchFieldsChanged) {
            rebuildBlindIndex(id, metaEntity, newSummary)
        }

        // 4. 历史快照
        snapshotChanges(
            id,
            metaEntity,
            oldSummary,
            if (changes.secret != null) newSecret else null,
            now
        )
    }

    // =========================== 生命周期 ===========================

    /**
     * 移入回收站（软删除）。
     * 原子写入：Metadata 软删除 + 盲索引清理 + 历史快照 + 活动记录。
     */
    override suspend fun moveToTrash(
        id: String, expectedVersion: Int
    ): AppResult<Unit> = unitOfWork.write("entry.moveToTrash") {
        val now = clock.now()
        val affected = entryDao().optimisticSoftDelete(id, expectedVersion, now, now)
        unitOfWork.checkAffectedRows(id, expectedVersion, affected)

        searchTokenDao().deleteByEntryId(id)

        // 活动记录
        entryActivityDao().insertStrict(
            EntryActivity(entryId = id, activityType = ActivityType.DELETE).toEntity(now)
        )
    }

    /**
     * 恢复回收站中的条目。
     *
     * 使用乐观锁版本校验，原子写入：恢复 + 版本自增 + 盲索引重建 + 活动记录。
     */
    override suspend fun restoreEntry(
        id: String, expectedVersion: Int
    ): AppResult<Unit> = unitOfWork.write("entry.restore") {
        val now = clock.now()
        val affected = entryDao().restoreOptimistic(id, expectedVersion, now)
        unitOfWork.checkAffectedRows(id, expectedVersion, affected)

        // 重建盲索引
        val metaEntity = entryDao().getById(id)
            ?: throw NotFound("entry:$id not found")
        val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
        rebuildBlindIndex(id, metaEntity, summary)

        // 活动记录
        entryActivityDao().insertStrict(
            EntryActivity(entryId = id, activityType = ActivityType.RESTORE).toEntity(now)
        )
    }

    // =========================== 搜索索引 ===========================

    /**
     * 重建所有条目的盲索引。
     *
     * 仅在以下情况实际执行重建：
     * - 索引不完整（[scanIndexStatus.isComplete] == false）
     * - [force] = true（如备份导入后）
     *
     * 完整性通过扫描实际数据库状态判断（已索引去重条目数 vs 活跃条目数），
     * 而非硬编码版本号或总行数。
     */
    override suspend fun rebuildIndex(force: Boolean): AppResult<Int> =
        unitOfWork.write("entry.rebuildIndex") {
            if (!force) {
                val scanResult = scanIndexStatus(
                    indexedEntryCount = searchTokenDao().countDistinctEntryIds(),
                    activeEntryCount = entryDao().countActive()
                )
                if (scanResult.isComplete) return@write 0
            }

            val metaEntities = entryDao().getActive()
        if (metaEntities.isEmpty()) return@write 0

        val entryIds = metaEntities.map { it.entryId }
            val credEntities = entrySecretDao().getByEntryIds(entryIds)
        val credMap = credEntities.associateBy { it.entryId }

            searchTokenDao().clear()

        var indexedCount = 0
        for (metaEntity in metaEntities) {
            val credEntity = credMap[metaEntity.entryId]
            val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
            val secret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
            val entry = EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
            val indexRecords = blindIndexer.index(entry.id, entry.toLookupFields())
            if (indexRecords.isNotEmpty()) {
                searchTokenDao().upsertAllForImport(indexRecords.toEntityList())
                indexedCount++
            }
        }

        indexedCount
    }

    // =========================== 使用记录 ===========================

    /**
     * 记录条目使用事件。
     * 仅插入 Activity 记录，使用统计由 Activity 表聚合查询提供，
     * 不再修改 Metadata 或增加 entryVersion。
     */
    override suspend fun recordUsage(
        entryId: String, type: ActivityType
    ): AppResult<Unit> = unitOfWork.write("entry.recordUsage") {
        val now = clock.now()
        entryActivityDao().insertStrict(
            EntryActivity(entryId = entryId, activityType = type).toEntity(now)
        )
    }

    // =========================== 内部方法 ===========================

    /**
     * 重建指定条目的盲索引。
     */
    private suspend fun AppDatabase.rebuildBlindIndex(
        id: String,
        metaEntity: EntryEntity,
        summary: EntrySummary
    ) {
        searchTokenDao().deleteByEntryId(id)
        val entry = metaEntity.toVaultEntry(summary)
        val indexRecords = blindIndexer.index(id, entry.toLookupFields())
        if (indexRecords.isNotEmpty()) {
            searchTokenDao().upsertAllForImport(indexRecords.toEntityList())
        }
    }

    /**
     * 写入历史快照。
     */
    private suspend fun AppDatabase.snapshotChanges(
        id: String,
        oldMetaEntity: EntryEntity,
        summary: EntrySummary,
        secret: EntrySecret?,
        now: Long
    ) {
        val newVersion = oldMetaEntity.version + 1
        val resolvedSecret = secret ?: entrySecretDao().getByEntryId(id)?.let {
            secretCodec.decrypt(it.secretBlob, it.entryId)
        } ?: EntrySecret.VaultData()
        val snapshotBlob = revisionCodec.encrypt(summary, resolvedSecret, id)

        entryRevisionDao().insertStrict(
            EntryRevisionEntity(
                revisionId = UuidCreator.getTimeOrderedEpoch().toString(),
                version = newVersion,
                entryId = id,
                snapshotBlob = snapshotBlob,
                changeType = RevisionType.VALUE_CHANGED.value,
                createdAt = now
            )
        )
    }

    private companion object {

        private fun List<com.aozijx.passly.security.search.BlindIndexRecord>.toEntityList(): List<SearchTokenEntity> =
            map { record ->
                SearchTokenEntity(
                    entryId = record.entryId,
                    field = record.field,
                    keywordHash = record.keywordHash,
                    gramLength = record.gramLength,
                    weight = record.weight
                )
            }

        private fun EntryActivity.toEntity(now: Long) =
            com.aozijx.passly.data.model.entity.EntryActivityEntity(
                activityId = activityId,
                entryId = entryId,
                activityType = activityType,
                createdAt = now
            )
    }
}

private fun EntryEntity.toVaultEntry(summary: EntrySummary): VaultEntry =
    EntryAggregateAssembler.assembleFromDatabase(this, summary, null)

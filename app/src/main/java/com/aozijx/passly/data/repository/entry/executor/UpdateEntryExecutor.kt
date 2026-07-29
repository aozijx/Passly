package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.NotFound
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.EntryActivityHelper
import com.aozijx.passly.data.repository.entry.internal.EntryBlindIndexHelper
import com.aozijx.passly.data.repository.entry.internal.EntryRevisionHelper
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.service.EntrySecretPolicy
import javax.inject.Inject

/**
 * 更新条目事务执行器。
 *
 * 原子写入：Metadata(含版本) + Credential + 盲索引 + 历史快照 + 活动记录。
 * 覆盖所有字段（title, username, password, email, notes, otp, card,
 * ssh, customFields 等），替代原有的多个单字段命令。
 */
class UpdateEntryExecutor @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val blindIndexHelper: EntryBlindIndexHelper,
    private val snapshotHelper: EntryRevisionHelper,
    private val activityHelper: EntryActivityHelper,
    private val clock: Clock
) {
    suspend fun execute(
        id: String,
        expectedVersion: Int,
        changes: EntryChanges
    ): AppResult<Unit> = transactionRunner.write("entry.update") {
        val metaEntity = entryQueryDao().getById(id)
            ?: throw NotFound("entry:$id not found")
        val oldSummary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
        val credEntity = entrySecretQueryDao().getByEntryId(id)
        val oldSecret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }

        val newSummary = changes.summary ?: oldSummary
        val newSecret = changes.secret ?: (oldSecret ?: EntrySecret())
        val now = clock.now()
        EntrySecretPolicy.requireValid(metaEntity.entryType, newSecret)

        // 1. 版本校验 + metadata 更新（原子操作）
        val metaBlob = summaryCodec.encrypt(newSummary, id)
        val capabilityFlags = EntryCapabilityFlags.computeFrom(
            secret = newSecret,
            hasAttachments = EntryCapabilityFlags.has(
                metaEntity.capabilityFlags,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
        val otpType = EntryCapabilityFlags.otpTypeFrom(newSecret)
        val affected = entryCommandDao().optimisticUpdate(
            id, expectedVersion, metaBlob, capabilityFlags, otpType, now
        )
        transactionRunner.checkAffectedRows(id, expectedVersion, affected)

        // 2. 写入 Secret（有变更或首次创建凭据时更新）
        if (changes.secret != null || oldSecret == null) {
            val credBlob = secretCodec.encrypt(newSecret, id)
            entrySecretCommandDao().updateBlob(id, credBlob)
        }

        // 3. 重建盲索引（仅搜索相关字段变化时）
        val searchFieldsChanged =
            oldSummary.title != newSummary.title || oldSummary.username != newSummary.username
        if (changes.summary != null && searchFieldsChanged) {
            val vaultEntry = EntryAggregateAssembler.assembleFromDatabase(
                metaEntity, newSummary, null
            )
            blindIndexHelper.rebuildForEntry(this, id, vaultEntry.toLookupFields())
        }

        // 4. 历史快照（保存 newSummary + newSecret，版本使用更新后的 EntryEntity.version）
        snapshotHelper.snapshotChanges(
            db = this,
            entryId = id,
            entryVersion = metaEntity.version + 1,
            summary = newSummary,
            secret = newSecret,
            now = now
        )

        // 5. 活动记录与数据更新处于同一事务，失败时一起回滚。
        activityHelper.recordActivity(this, id, ActivityType.UPDATE, now)
    }
}

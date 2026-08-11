package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.EntryActivityHelper
import com.aozijx.passly.data.repository.entry.internal.EntryBlindIndexHelper
import com.aozijx.passly.data.repository.entry.internal.EntryRevisionHelper
import com.aozijx.passly.data.repository.entry.internal.SensitiveFieldPersistence
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.extractHighSensitivity
import com.aozijx.passly.domain.entry.model.mergeWith
import com.aozijx.passly.domain.entry.model.withHighSensitivity
import com.aozijx.passly.domain.entry.model.withoutHighSensitivity
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
    private val sensitiveFieldPersistence: SensitiveFieldPersistence,
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
            ?: throw NotFound()
        val oldSummary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
        val credEntity = entrySecretQueryDao().getByEntryId(id)
        val oldSecret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
        val newSummary = changes.summary ?: oldSummary
        val changedHighSensitivitySecret = changes.secret?.extractHighSensitivity()
            ?: changes.highSensitivitySecret
        val sensitiveValuesChanged = changedHighSensitivitySecret != null
        val oldHighSensitivitySecret = if (sensitiveValuesChanged) {
            sensitiveFieldPersistence.readAllForMutation(this, id)
        } else {
            com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret.EMPTY
        }
        val newHighSensitivitySecret = when {
            changedHighSensitivitySecret != null ->
                oldHighSensitivitySecret.mergeWith(changedHighSensitivitySecret)
            else -> oldHighSensitivitySecret
        }
        val newSecretInput = changes.secret ?: (oldSecret ?: EntrySecret())
        val newFullSecret = newSecretInput.withHighSensitivity(newHighSensitivitySecret)
        val newPersistedSecret = newFullSecret.withoutHighSensitivity()
        val now = clock.now()
        if (changes.secret != null || changes.highSensitivitySecret != null) {
            EntrySecretPolicy.requireValid(metaEntity.entryType, newFullSecret)
        }

        // 1. 版本校验 + metadata 更新（原子操作）
        val metaBlob = summaryCodec.encrypt(newSummary, id)
        val capabilityFlags = EntryCapabilityFlags.computeFrom(
            secret = newPersistedSecret,
            hasAttachments = EntryCapabilityFlags.has(
                metaEntity.capabilityFlags,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
        val otpType = EntryCapabilityFlags.otpTypeFrom(newFullSecret)
        val affected = entryCommandDao().optimisticUpdate(
            id, expectedVersion, metaBlob, capabilityFlags, otpType, now
        )
        transactionRunner.checkAffectedRows(affected)

        // 2. 写入 Secret（有变更或首次创建凭据时更新）
        if (changes.secret != null || changes.highSensitivitySecret != null || oldSecret == null) {
            val credBlob = secretCodec.encrypt(newPersistedSecret, id)
            entrySecretCommandDao().updateBlob(id, credBlob)
            sensitiveFieldPersistence.replaceAll(this, id, newHighSensitivitySecret)
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
            secret = newPersistedSecret,
            now = now
        )

        // 5. 活动记录与数据更新处于同一事务，失败时一起回滚。
        activityHelper.recordActivity(this, id, ActivityType.UPDATE, now)
        if (sensitiveValuesChanged) {
            activityHelper.recordActivity(this, id, ActivityType.SENSITIVE_CHANGE, now)
        }
    }
}

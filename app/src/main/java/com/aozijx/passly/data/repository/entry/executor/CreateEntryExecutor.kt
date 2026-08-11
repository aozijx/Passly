package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.EntryActivityHelper
import com.aozijx.passly.data.repository.entry.internal.EntryBlindIndexHelper
import com.aozijx.passly.data.repository.entry.internal.EntryRevisionHelper
import com.aozijx.passly.data.repository.entry.internal.SensitiveFieldPersistence
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.extractHighSensitivity
import com.aozijx.passly.domain.entry.model.withoutHighSensitivity
import com.aozijx.passly.domain.entry.service.EntrySecretPolicy
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject

/**
 * 创建条目事务执行器。
 *
 * 原子写入：Metadata + Credential + 盲索引 + 历史快照 + 活动记录。
 */
class CreateEntryExecutor @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val sensitiveFieldPersistence: SensitiveFieldPersistence,
    private val blindIndexHelper: EntryBlindIndexHelper,
    private val snapshotHelper: EntryRevisionHelper,
    private val activityHelper: EntryActivityHelper,
    private val clock: Clock
) {
    suspend fun execute(entry: EntryAggregate): AppResult<EntryId> =
        transactionRunner.write("entry.create") {
            val now = clock.now()
            val entryId = entry.id.ifEmpty { UuidCreator.getTimeOrderedEpoch().toString() }

            val fullSecret = entry.fullSecret
            val highSensitivitySecret = entry.highSensitivitySecret ?: fullSecret.extractHighSensitivity()
            val persistedSecret = fullSecret.withoutHighSensitivity()

            EntrySecretPolicy.requireValid(entry.entryType, fullSecret)
            val metaBlob = summaryCodec.encrypt(entry.summary, entryId)
            val credBlob = secretCodec.encrypt(persistedSecret, entryId)

            val capabilityFlags = EntryCapabilityFlags.computeFrom(fullSecret)
            val otpType = EntryCapabilityFlags.otpTypeFrom(fullSecret)

            val metaEntity = EntryEntity(
                entryId = entryId,
                entryType = entry.entryType,
                capabilityFlags = capabilityFlags,
                otpType = otpType,
                summaryBlob = metaBlob,
                createdAt = now,
                updatedAt = now
            )
            val credEntity = EntrySecretEntity(
                entryId = entryId,
                secretBlob = credBlob
            )

            entryCommandDao().insertStrict(metaEntity)
            entrySecretCommandDao().insertStrict(credEntity)
            sensitiveFieldPersistence.replaceAll(this, entryId, highSensitivitySecret)

            // 盲索引
            blindIndexHelper.rebuildForEntry(this, entryId, entry.toLookupFields())

            // 历史快照（version = 1，与 EntryEntity.version 一致）
            snapshotHelper.snapshotChanges(
                db = this,
                entryId = entryId,
                entryVersion = 1,
                summary = entry.summary,
                secret = fullSecret,
                now = now
            )

            // 活动记录
            activityHelper.recordActivity(this, entryId, ActivityType.CREATE, now)

            EntryId(entryId)
        }
}

package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.revision.RevisionType
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史修订协作器。
 *
 * 封装修订的加密与写入，供各 Command Executor 在事务内调用。
 */
@Singleton
class EntryRevisionHelper @Inject constructor(
    private val revisionCodec: EntryRevisionCodec
) {

    /**
     * 写入一条历史修订，版本号直接使用 [entryVersion]（即 EntryEntity.version）。
     */
    suspend fun snapshotChanges(
        db: AppDatabase,
        entryId: String,
        entryVersion: Int,
        summary: EntrySummary,
        secret: EntrySecret,
        now: Long
    ) {
        val entryBlob = revisionCodec.encrypt(summary, secret, entryId)
        with(db) {
            entryRevisionCommandDao().insertStrict(
                EntryRevisionEntity(
                    revisionId = UuidCreator.getTimeOrderedEpoch().toString(),
                    version = entryVersion,
                    entryId = entryId,
                    entryBlob = entryBlob,
                    changeType = RevisionType.VALUE_CHANGED.value,
                    createdAt = now
                )
            )
        }
    }
}

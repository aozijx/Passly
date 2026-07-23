package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.codec.snapshot.EntrySnapshotCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.snapshot.RevisionType
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史快照协作器。
 *
 * 封装快照的加密与写入，供各 Command Executor 在事务内调用。
 */
@Singleton
class EntrySnapshotHelper @Inject constructor(
    private val revisionCodec: EntrySnapshotCodec
) {

    /**
     * 写入一条历史快照，版本号为 [oldVersion] + 1。
     */
    suspend fun snapshotChanges(
        db: AppDatabase,
        entryId: String,
        oldVersion: Int,
        summary: EntrySummary,
        secret: EntrySecret,
        now: Long
    ) {
        val newVersion = oldVersion + 1
        val snapshotBlob = revisionCodec.encrypt(summary, secret, entryId)
        with(db) {
            entrySnapshotCommandDao().insertIdempotent(
                EntryRevisionEntity(
                    revisionId = UuidCreator.getTimeOrderedEpoch().toString(),
                    version = newVersion,
                    entryId = entryId,
                    snapshotBlob = snapshotBlob,
                    changeType = RevisionType.VALUE_CHANGED.value,
                    createdAt = now
                )
            )
        }
    }
}

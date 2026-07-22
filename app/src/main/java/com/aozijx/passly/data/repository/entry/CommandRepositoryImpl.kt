package com.aozijx.passly.data.repository.entry

import androidx.room.withTransaction
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.Conflict
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.lookup.toLookupFields
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.repository.database.TransactionOperator
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.aozijx.passly.security.search.BlindIndexer
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 命令 Repository 实现：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作统一处理加解密、关联表和盲索引的同步更新。
 *
 * 每个命令方法明确指定修改的字段，不接收完整 [VaultEntry]，
 * 防止 UI 层将旧快照错误地回写到数据库。
 */
@Singleton
class CommandRepositoryImpl @Inject constructor(
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider,
    private val transactionOperator: TransactionOperator,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val blindIndexer: BlindIndexer,
    private val clock: Clock
) : CommandRepository {

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.insert") {
                withTransaction {
                    val entryId = entry.id.ifEmpty { UuidCreator.getTimeOrderedEpoch().toString() }
                    val now = clock.now()
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

                    // 构建并写入盲索引
                    val indexedEntry = entry.copy(metadata = entry.metadata.copy(entryId = entryId))
                    val indexRecords = blindIndexer.index(entryId, indexedEntry.toLookupFields())
                    if (indexRecords.isNotEmpty()) {
                        lookupIndexDao().insertAll(indexRecords.toEntityList())
                    }

                    0L
                }
            }
        }
    }

    // ============================== 元数据字段命令 ==============================

    override suspend fun updateTitle(
        id: String, expectedVersion: Int, title: String
    ): AppResult<Unit> = updateMetadataField(id, expectedVersion, TAG) { meta ->
        meta.copy(title = title)
    }

    override suspend fun updateUsername(
        id: String, expectedVersion: Int, username: String
    ): AppResult<Unit> = updateMetadataField(id, expectedVersion, TAG) { meta ->
        meta.copy(username = username)
    }

    override suspend fun toggleFavorite(
        id: String, expectedVersion: Int
    ): AppResult<Unit> = updateMetadataField(id, expectedVersion, TAG) { meta ->
        meta.copy(favorite = !meta.favorite)
    }

    override suspend fun setIcon(
        id: String, expectedVersion: Int, iconPath: String?
    ): AppResult<Unit> =
        updateMetadataField(id, expectedVersion, TAG, rebuildIndex = false) { meta ->
            meta.copy(icon = iconPath)
        }

    override suspend fun updateWebsite(
        id: String, expectedVersion: Int, website: WebsiteInfo?
    ): AppResult<Unit> = updateMetadataField(id, expectedVersion, TAG) { meta ->
        meta.copy(website = website)
    }

    // ============================== 凭据字段命令 ==============================

    override suspend fun updatePassword(
        id: String, expectedVersion: Int, password: String
    ): AppResult<Unit> =
        updateCredentialField(id, expectedVersion, TAG, rebuildIndex = false) { cred ->
            cred.copy(password = password)
        }

    override suspend fun updateEmail(
        id: String, expectedVersion: Int, email: String
    ): AppResult<Unit> = updateCredentialField(id, expectedVersion, TAG) { cred ->
        cred.copy(email = email)
    }

    override suspend fun updateNotes(
        id: String, expectedVersion: Int, notes: String
    ): AppResult<Unit> =
        updateCredentialField(id, expectedVersion, TAG, rebuildIndex = false) { cred ->
            cred.copy(notes = notes)
        }

    // ============================== 生命周期 ==============================

    override suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.moveToTrash") {
                withTransaction {
                    val oldMetaEntity = metadataDao().getById(id)
                        ?: return@withTransaction
                    if (oldMetaEntity.entryVersion != expectedVersion) {
                        throw Conflict(
                            "entry:$id version mismatch: expected=$expectedVersion, actual=${oldMetaEntity.entryVersion}"
                        )
                    }
                    metadataDao().softDelete(id, clock.now())
                    lookupIndexDao().deleteByEntryId(id)
                }
            }
        }
    }

    // ============================== 搜索索引 ==============================

    override suspend fun rebuildIndex(): AppResult<Int> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.rebuildIndex") {
                withTransaction {
                    val metaEntities = metadataDao().getActive()
                    if (metaEntities.isEmpty()) return@withTransaction 0

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

                    AppLog.i(
                        TAG, "Rebuilt blind index for $indexedCount/${
                            metaEntities.size
                        } entries"
                    )

                    indexedCount
                }
            }
        }
    }

    // ============================== 内部辅助 ==============================

    /**
     * 更新元数据中的某个字段，并同步更新盲索引。
     *
     * @param id 条目 ID
     * @param expectedVersion 预期版本号
     * @param logTag 日志标签
     * @param rebuildIndex 是否重建盲索引（仅当修改的字段属于搜索字段时设为 true）
     * @param transform 对解密后的 [VaultMetadata] 进行转换
     */
    private suspend fun updateMetadataField(
        id: String, expectedVersion: Int, logTag: String,
        rebuildIndex: Boolean = true,
        transform: (VaultMetadata) -> VaultMetadata
    ): AppResult<Unit> = writeOperation(id, expectedVersion, logTag) { snapshot ->
        val newMeta = transform(snapshot.meta)
        val metaBlob = cryptoMapper.encryptMetadata(newMeta, id)

        val newMetaEntity = snapshot.metaEntity.copy(
            metadataBlob = metaBlob,
            entryVersion = snapshot.metaEntity.entryVersion + 1,
            updatedAt = clock.now()
        )
        metadataDao().update(newMetaEntity)

        if (rebuildIndex) {
            rebuildBlindIndex(id, snapshot.metaEntity, snapshot.cred)
        }
    }

    /**
     * 更新凭据中的某个字段。凭据字段通常不参与搜索索引，默认不重建盲索引。
     *
     * @param id 条目 ID
     * @param expectedVersion 预期版本号
     * @param logTag 日志标签
     * @param rebuildIndex 是否重建盲索引（仅 email 等搜索字段设为 true）
     * @param transform 对解密后的 [VaultCredential] 进行转换
     */
    private suspend fun updateCredentialField(
        id: String, expectedVersion: Int, logTag: String,
        rebuildIndex: Boolean = false,
        transform: (VaultCredential) -> VaultCredential
    ): AppResult<Unit> = writeOperation(id, expectedVersion, logTag) { snapshot ->
        val newCred = transform(snapshot.cred)
        val credBlob = cryptoMapper.encryptCredential(newCred, id)

        val newCredEntity = snapshot.credEntity?.copy(credentialBlob = credBlob)
            ?: VaultCredentialEntity(entryId = id, credentialBlob = credBlob)
        credentialDao().update(newCredEntity)

        if (rebuildIndex) {
            rebuildBlindIndex(id, snapshot.metaEntity, newCred)
        }
    }

    /**
     * 通用的写操作模板：读取、版本校验、执行、提交。
     * [block] 使用 [AppDatabase] 作为接收者，确保内部 DAO 方法可访问。
     */
    private suspend fun writeOperation(
        id: String, expectedVersion: Int, logTag: String,
        block: suspend AppDatabase.(EntrySnapshot) -> Unit
    ): AppResult<Unit> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.$logTag") {
                withTransaction {
                    val snapshot = readEntry(id) ?: return@withTransaction
                    if (snapshot.metaEntity.entryVersion != expectedVersion) {
                        throw Conflict(
                            "entry:$id version mismatch: expected=$expectedVersion, actual=${snapshot.metaEntity.entryVersion}"
                        )
                    }
                    block(snapshot)
                }
            }
        }
    }

    /**
     * 读取并解密指定条目的完整数据。
     * 作为 [AppDatabase] 扩展函数，确保 DAO 方法在接收者作用域中可访问。
     */
    private suspend fun AppDatabase.readEntry(id: String): EntrySnapshot? {
        val metaEntity = metadataDao().getById(id) ?: return null
        val meta = cryptoMapper.decryptMetadata(metaEntity)
        val credEntity = credentialDao().getByEntryId(id)
        val cred = if (credEntity != null) cryptoMapper.decryptCredential(credEntity)
        else VaultCredential(entryId = id)
        return EntrySnapshot(metaEntity, meta, cred, credEntity)
    }

    /**
     * 重建指定条目的盲索引。
     * 作为 [AppDatabase] 扩展函数，确保 lookupIndexDao() 在接收者作用域中可访问。
     */
    private suspend fun AppDatabase.rebuildBlindIndex(
        id: String,
        metaEntity: VaultMetadataEntity,
        cred: VaultCredential
    ) {
        // 组装 VaultEntry 以调用 toLookupFields()
        val meta = cryptoMapper.decryptMetadata(metaEntity)
        val entry = VaultEntry(meta, cred).copy(
            metadata = meta.copy(entryId = id)
        )
        lookupIndexDao().deleteByEntryId(id)
        val indexRecords = blindIndexer.index(id, entry.toLookupFields())
        if (indexRecords.isNotEmpty()) {
            lookupIndexDao().insertAll(indexRecords.toEntityList())
        }
    }

    /**
     * 快照：缓存解密后的条目数据，避免多次读取。
     */
    private data class EntrySnapshot(
        val metaEntity: VaultMetadataEntity,
        val meta: VaultMetadata,
        val cred: VaultCredential,
        val credEntity: VaultCredentialEntity?
    )

    private companion object {
        private const val TAG = "CommandRepositoryImpl"

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
    }
}

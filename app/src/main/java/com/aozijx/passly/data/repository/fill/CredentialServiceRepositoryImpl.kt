package com.aozijx.passly.data.repository.fill

import com.aozijx.passly.data.local.DatabaseSessionManager
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.repository.vault.internal.ifLockedReturn
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.CredentialRepository
import com.aozijx.passly.security.crypto.FieldEncryptor
import com.aozijx.passly.security.crypto.VaultLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CredentialRepository 实现：封装 Password 类型条目的查询、保存、解密、使用统计。
 *
 * Legacy Autofill 和 Modern CredentialManager 两条路径共享此实现。
 * 通过 [DatabaseSessionManager] 访问 DAO，与 VaultRepository 等保持一致的 Hilt 注入模式。
 */
@Singleton
class CredentialServiceRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSessionManager,
    private val lockManager: VaultLockManager,
    private val fieldEncryptor: FieldEncryptor
) : CredentialRepository {

    override fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate> = runBlocking(Dispatchers.IO) {
        lockManager.ifLockedReturn { return@runBlocking emptyList() }
        sessionManager.withDatabase {
            vaultEntryDao().getAll().map { it.toDomain(fieldEncryptor) }
                .filter { it.entryType == EntryType.PASSWORD.value }
                .map { entry ->
                    CredentialCandidate(
                        entry = entry,
                        score = MatchType.UNKNOWN.score,
                        matchedBy = MatchType.UNKNOWN,
                        matchedDomain = entry.associatedDomain,
                        matchedPackage = entry.associatedAppPackage
                    )
                }
        }
    }

    override fun getById(entryId: Int): VaultEntry? = runBlocking(Dispatchers.IO) {
        lockManager.ifLockedReturn { return@runBlocking null }
        sessionManager.withDatabase {
            vaultEntryDao().getEntryById(entryId)?.toDomain(fieldEncryptor)
        }
    }

    override fun getByIds(entryIds: List<Int>): List<VaultEntry> =
        runBlocking(Dispatchers.IO) {
            lockManager.ifLockedReturn { return@runBlocking emptyList() }
            sessionManager.withDatabase {
                vaultEntryDao().getEntriesByIds(entryIds).map { it.toDomain(fieldEncryptor) }
            }
        }

    override fun updateLastUsed(entryId: Int) {
        runBlocking(Dispatchers.IO) {
            lockManager.ifLockedReturn { return@runBlocking }
            sessionManager.withDatabase {
                val dao = vaultEntryDao()
                val entity = dao.getEntryById(entryId) ?: return@withDatabase
                val domain = entity.toDomain(fieldEncryptor)
                val updated = domain.copy(
                    usageCount = domain.usageCount + 1,
                    lastUsedAt = System.currentTimeMillis()
                )
                dao.update(updated.toEntity(fieldEncryptor))
            }
        }
    }

    override fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = runBlocking(Dispatchers.IO) {
        lockManager.ifLockedReturn { return@runBlocking false }
        sessionManager.withDatabase {
            val dao = vaultEntryDao()
            val existing = dao.getAll().firstOrNull {
                it.toDomain(fieldEncryptor).username == usernameValue
            }
            if (existing != null) {
                val domain = existing.toDomain(fieldEncryptor)
                dao.update(
                    domain.copy(
                        password = passwordValue,
                        associatedAppPackage = packageName,
                        associatedDomain = webDomain,
                        title = pageTitle ?: usernameValue
                    ).toEntity(fieldEncryptor)
                )
            } else {
                dao.insert(
                    VaultEntry(
                        title = pageTitle ?: usernameValue,
                        username = usernameValue,
                        password = passwordValue,
                        category = "",
                        entryType = EntryType.PASSWORD.value,
                        associatedAppPackage = packageName,
                        associatedDomain = webDomain
                    ).toEntity(fieldEncryptor)
                )
            }
        }
        true
    }
}

package com.aozijx.passly.data.repository.fill

import com.aozijx.passly.data.local.DatabaseSessionManager
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.autofill.AutofillConfiguration
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.CredentialRepository
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
) : CredentialRepository {

    override fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = runBlocking(Dispatchers.IO) {
        if (lockManager.isLocked()) return@runBlocking false
        try {
            sessionManager.withDatabase<Unit> {
                val dao = vaultEntryDao()
                val existing = if (usernameValue.isNotBlank()) {
                    dao.getAll().firstOrNull { it.toDomain().username == usernameValue }
                } else null

                if (existing != null) {
                    val domain = existing.toDomain()
                    val updated = domain.copy(
                        password = passwordValue,
                        updatedAt = System.currentTimeMillis(),
                    ).toEntity()
                    dao.update(updated)
                } else {
                    val entry = VaultEntry(
                        title = pageTitle ?: webDomain ?: packageName ?: "Unknown",
                        username = usernameValue,
                        password = passwordValue,
                        associatedAppPackage = packageName,
                        associatedDomain = webDomain,
                        entryType = EntryType.PASSWORD.value,
                        category = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ).toEntity()
                    dao.insert(entry)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun decrypt(entry: VaultEntry): VaultEntry? {
        if (lockManager.isLocked()) return null
        return entry
    }

    override fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate> = runBlocking(Dispatchers.IO) {
        if (lockManager.isLocked()) return@runBlocking emptyList()

        val allEntries = sessionManager.withDatabase {
            vaultEntryDao().getAll().map { it.toDomain() }
                .filter {
                    it.entryType == EntryType.PASSWORD.value &&
                            AutofillConfiguration.isAutofillSupported(EntryType.fromValue(it.entryType))
                }
        }

        if (allEntries.isEmpty()) return@runBlocking emptyList()

        val normalizedDomain = webDomain?.lowercase()?.trim('/')

        val candidates = allEntries.mapNotNull { entry ->
            val entryDomain = entry.associatedDomain?.lowercase()?.trim('/')

            val matchInfo = when {
                packageName != null && entry.associatedAppPackage == packageName -> {
                    val domain =
                        if (!normalizedDomain.isNullOrBlank() && entryDomain == normalizedDomain) {
                            normalizedDomain
                        } else null
                    Triple(MatchType.PACKAGE_NAME, domain, packageName)
                }

                !normalizedDomain.isNullOrBlank() && entryDomain == normalizedDomain -> {
                    Triple(MatchType.WEB_DOMAIN, normalizedDomain, null)
                }

                else -> null
            }

            if (matchInfo != null) {
                val (matchedBy, matchedDomain, matchedPackage) = matchInfo
                CredentialCandidate(
                    entry = entry,
                    score = matchedBy.score,
                    matchedBy = matchedBy,
                    matchedDomain = matchedDomain,
                    matchedPackage = matchedPackage,
                )
            } else {
                null
            }
        }
            .filter { it.matchedBy != MatchType.UNKNOWN }
            .sortedWith(AutofillConfiguration::compareCandidates)
            .take(AutofillConfiguration.MAX_CANDIDATES)

        candidates
    }

    override fun getById(entryId: Int): VaultEntry? {
        if (lockManager.isLocked()) return null
        val entity = runBlocking(Dispatchers.IO) {
            sessionManager.withDatabase {
                vaultEntryDao().getEntryById(entryId)
            }
        } ?: return null
        return entity.toDomain()
    }

    override fun getByIds(entryIds: List<Int>): List<VaultEntry> {
        if (lockManager.isLocked()) return emptyList()
        return runBlocking(Dispatchers.IO) {
            sessionManager.withDatabase {
                vaultEntryDao().getEntriesByIds(entryIds).map { it.toDomain() }
            }
        }
    }

    override fun updateLastUsed(entry: VaultEntry) {
        if (lockManager.isLocked()) return
        runBlocking(Dispatchers.IO) {
            sessionManager.withDatabase {
                val dao = vaultEntryDao()
                val entity = dao.getEntryById(entry.id)
                if (entity != null) {
                    val updated = entry.copy(
                        usageCount = (entity.toDomain().usageCount) + 1,
                        lastUsedAt = System.currentTimeMillis(),
                    )
                    dao.update(updated.toEntity())
                }
            }
        }
    }
}
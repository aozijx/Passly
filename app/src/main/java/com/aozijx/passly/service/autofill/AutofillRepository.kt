package com.aozijx.passly.service.autofill

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.policy.AutofillTitlePolicy
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import com.aozijx.passly.service.autofill.engine.AutofillStructureParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自动填充数据仓库
 */
object AutofillRepository {
    private const val TAG = "AutofillRepo"
    private const val CANDIDATE_QUERY_LIMIT = 24
    private const val EXISTING_MATCH_QUERY_LIMIT = 48
    private const val SLOW_DB_QUERY_MS = 120L
    private const val SLOW_ENTRY_MATCH_MS = 120L
    private const val SLOW_SAVE_FLOW_MS = 200L

    enum class MatchType {
        APP,
        DOMAIN,
        UNKNOWN
    }

    data class AutofillCandidate(
        val entry: VaultEntry,
        val matchType: MatchType,
        val rank: Int
    )

    suspend fun updateUsageStats(context: Context, entry: VaultEntry) = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val updatedEntry = entry.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = entry.usageCount + 1
            )
            dao.update(updatedEntry.toEntity())
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to update usage count for ${entry.id}", e)
        }
    }

    suspend fun findMatchingEntries(context: Context, packageName: String?, webDomain: String?): List<VaultEntry> = withContext(Dispatchers.IO) {
        findMatchingCandidates(context, packageName, webDomain).map { it.entry }
    }

    suspend fun findMatchingCandidates(
        context: Context,
        packageName: String?,
        webDomain: String?
    ): List<AutofillCandidate> = withContext(Dispatchers.IO) {
        try {
            if (packageName.isNullOrBlank() && webDomain.isNullOrBlank()) {
                return@withContext emptyList()
            }

            EntryTypeStrategyRegistry.ensureRegistered()
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val queryStart = System.currentTimeMillis()
            val entries = dao.findAutofillCandidates(
                packageName = packageName,
                webDomain = webDomain,
                limit = CANDIDATE_QUERY_LIMIT
            ).toDomainList()
            val queryCost = System.currentTimeMillis() - queryStart
            if (queryCost >= SLOW_DB_QUERY_MS) {
                Logcat.w(TAG, "findMatchingEntries query slow: ${queryCost}ms, candidates=${entries.size}")
            }

            val normalizedPackage = normalizePackageName(packageName)
            val normalizedDomain = AutofillStructureParser.normalizeDomain(webDomain)

            entries.asSequence()
                .mapNotNull { entry ->
                    if (!supportsAutofill(entry)) return@mapNotNull null

                    val matchType = when {
                        isPackageMatch(entry.associatedAppPackage, normalizedPackage) -> MatchType.APP
                        isDomainMatch(entry.associatedDomain, normalizedDomain) -> MatchType.DOMAIN
                        else -> MatchType.UNKNOWN
                    }

                    if (matchType == MatchType.UNKNOWN) return@mapNotNull null

                    val rank = if (matchType == MatchType.APP) 0 else 1
                    AutofillCandidate(entry = entry, matchType = matchType, rank = rank)
                }
                .sortedWith(
                    compareBy<AutofillCandidate> { it.rank }
                        .thenByDescending { it.entry.favorite }
                        .thenByDescending { it.entry.usageCount }
                        .thenByDescending { it.entry.updatedAt ?: it.entry.createdAt ?: 0L }
                )
                .take(5)
                .toList()
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to find matching entries", e)
            emptyList()
        }
    }

    suspend fun saveOrUpdateEntry(
        context: Context,
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val saveStart = System.currentTimeMillis()
            EntryTypeStrategyRegistry.ensureRegistered()

            // 核心修复：直接使用 applicationContext 避免潜在的 Context 泄露
            val appContext = context.applicationContext
            val dao = AppDatabase.getDatabase(appContext).vaultDao()
            
            // 1. 先通过包名/域名匹配现有条目
            val queryStart = System.currentTimeMillis()
            val candidateEntries = dao.findAutofillCandidates(
                packageName = packageName,
                webDomain = webDomain,
                limit = EXISTING_MATCH_QUERY_LIMIT
            ).toDomainList()
            val queryCost = System.currentTimeMillis() - queryStart
            if (queryCost >= SLOW_DB_QUERY_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry query slow: ${queryCost}ms, candidates=${candidateEntries.size}")
            }

            val matchStart = System.currentTimeMillis()
            val normalizedPackage = normalizePackageName(packageName)
            val normalizedDomain = AutofillStructureParser.normalizeDomain(webDomain)
            val existing = candidateEntries.find { entry ->
                if (!supportsAutofill(entry)) return@find false

                val scopeMatch = isPackageMatch(entry.associatedAppPackage, normalizedPackage) ||
                    isDomainMatch(entry.associatedDomain, normalizedDomain)
                if (scopeMatch) {
                    val decUser = try { CryptoManager.decrypt(entry.username) } catch (_: Exception) { null }
                    decUser == usernameValue
                } else false
            }
            val matchCost = System.currentTimeMillis() - matchStart
            if (matchCost >= SLOW_ENTRY_MATCH_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry username match slow: ${matchCost}ms")
            }

            val encUser = CryptoManager.encrypt(usernameValue)
            val encPass = CryptoManager.encrypt(passwordValue)
            
            if (existing != null) {
                // 更新逻辑
                val updatedEntry = existing.copy(
                    password = encPass,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updatedEntry.toEntity())
                Logcat.i(TAG, "Updated existing account: $usernameValue")
            } else {
                // 新建逻辑
                val appLabel = packageName?.let { pkg ->
                    try {
                        val info = appContext.packageManager.getApplicationInfo(pkg, 0)
                        appContext.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) { null }
                }

                val title = AutofillTitlePolicy.getSmartTitle(
                    pageTitle = pageTitle,
                    domain = webDomain,
                    appLabel = appLabel,
                    packageName = packageName,
                    strings = AutofillTitlePolicy.AutofillTitleStrings(
                        appFallback = appContext.getString(R.string.autofill_title_app_fallback),
                        lateNight = appContext.getString(R.string.autofill_title_late_night),
                        morning = appContext.getString(R.string.autofill_title_morning),
                        noon = appContext.getString(R.string.autofill_title_noon),
                        afternoon = appContext.getString(R.string.autofill_title_afternoon),
                        evening = appContext.getString(R.string.autofill_title_evening),
                        newEntry = appContext.getString(R.string.autofill_title_new_entry)
                    )
                )

                val passwordStrategy = resolveStrategy(EntryType.PASSWORD.value)
                val fallbackCategory = appContext.getString(R.string.category_autofill).ifBlank { "自动填充" }

                // 使用策略提供的默认值（例如建议分类）初始化自动保存条目
                val newEntry = VaultEntry(
                    title = title,
                    username = encUser,
                    password = encPass,
                    category = passwordStrategy?.suggestedCategory().orEmpty().ifBlank { fallbackCategory },
                    associatedAppPackage = packageName,
                    associatedDomain = webDomain,
                    entryType = EntryType.PASSWORD.value,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val initializedEntry = passwordStrategy?.initializeDefaults(newEntry) ?: newEntry
                val finalizedEntry = initializedEntry.copy(
                    category = initializedEntry.category.ifBlank { fallbackCategory },
                    entryType = EntryType.PASSWORD.value
                )

                val validationError = passwordStrategy?.validateRequiredFields(finalizedEntry)
                    ?: passwordStrategy?.validateFieldContent(finalizedEntry)
                if (validationError != null) {
                    Logcat.w(TAG, "Autofill entry validation warning: $validationError")
                }
                
                // 关键点：执行插入操作
                dao.insert(finalizedEntry.toEntity())
                Logcat.i(TAG, "Successfully saved new entry to DB: $title")
            }

            val saveCost = System.currentTimeMillis() - saveStart
            if (saveCost >= SLOW_SAVE_FLOW_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry slow: ${saveCost}ms")
            }
            return@withContext true
        } catch (e: Exception) {
            Logcat.e(TAG, "Save to database failed!", e)
            return@withContext false
        }
    }

    private fun resolveStrategy(entryTypeValue: Int) = runCatching {
        EntryTypeStrategyFactory.getStrategy(EntryType.fromValue(entryTypeValue))
    }.getOrNull()

    private fun supportsAutofill(entry: VaultEntry): Boolean {
        val entryType = EntryType.fromValue(entry.entryType)
        return resolveStrategy(entry.entryType)?.supportsAutofill() ?: entryType.supportsAutofill()
    }

    private fun normalizePackageName(packageName: String?): String? {
        return packageName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private fun isPackageMatch(entryPackage: String?, normalizedRequestPackage: String?): Boolean {
        if (normalizedRequestPackage == null) return false
        return entryPackage?.trim()?.lowercase() == normalizedRequestPackage
    }

    private fun isDomainMatch(entryDomain: String?, normalizedRequestDomain: String?): Boolean {
        if (normalizedRequestDomain == null) return false
        val normalizedEntryDomain = AutofillStructureParser.normalizeDomain(entryDomain) ?: return false
        return normalizedEntryDomain == normalizedRequestDomain ||
            normalizedEntryDomain.endsWith(".$normalizedRequestDomain") ||
            normalizedRequestDomain.endsWith(".$normalizedEntryDomain")
    }
}



package com.aozijx.passly.data.repository

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.AutofillMatchType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.policy.AutofillTitlePolicy
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import com.aozijx.passly.service.autofill.engine.AutofillStructureParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutofillServiceDataRepository(
    context: Context
) : AutofillServiceRepository {

    private val appContext = context.applicationContext

    private companion object {
        private const val TAG = "AutofillRepo"
        private const val CANDIDATE_QUERY_LIMIT = 24
        private const val EXISTING_MATCH_QUERY_LIMIT = 48
        private const val SLOW_DB_QUERY_MS = 120L
        private const val SLOW_ENTRY_MATCH_MS = 120L
        private const val SLOW_SAVE_FLOW_MS = 200L
    }

    override suspend fun updateUsageStats(entry: VaultEntry) = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(appContext).vaultEntryDao()
            val updatedEntry = entry.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = entry.usageCount + 1
            )
            dao.update(updatedEntry.toEntity())
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to update usage count for ${entry.id}", e)
        }
    }

    override suspend fun getEntryById(entryId: Int): VaultEntry? = withContext(Dispatchers.IO) {
        runCatching {
            AppDatabase.getDatabase(appContext).vaultEntryDao().getEntryById(entryId)?.toDomain()
        }.getOrElse {
            Logcat.e(TAG, "Failed to load entry by id=$entryId", it)
            null
        }
    }

    override suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntry> = withContext(Dispatchers.IO) {
        if (entryIds.isEmpty()) return@withContext emptyList()
        runCatching {
            val order = entryIds.withIndex().associate { it.value to it.index }
            AppDatabase.getDatabase(appContext)
                .vaultEntryDao()
                .getEntriesByIds(entryIds)
                .toDomainList()
                .sortedBy { order[it.id] ?: Int.MAX_VALUE }
        }.getOrElse {
            Logcat.e(TAG, "Failed to load entries by ids", it)
            emptyList()
        }
    }

    override suspend fun findMatchingCandidates(
        packageName: String?,
        webDomain: String?
    ): List<AutofillCandidate> = withContext(Dispatchers.IO) {
        try {
            if (packageName.isNullOrBlank() && webDomain.isNullOrBlank()) {
                return@withContext emptyList()
            }

            EntryTypeStrategyRegistry.ensureRegistered()
            val dao = AppDatabase.getDatabase(appContext).vaultEntryDao()
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
                        isPackageMatch(entry.associatedAppPackage, normalizedPackage) -> AutofillMatchType.APP
                        isDomainMatch(entry.associatedDomain, normalizedDomain) -> AutofillMatchType.DOMAIN
                        else -> AutofillMatchType.UNKNOWN
                    }

                    if (matchType == AutofillMatchType.UNKNOWN) return@mapNotNull null

                    val rank = if (matchType == AutofillMatchType.APP) 0 else 1
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

    override suspend fun saveOrUpdateEntry(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val saveStart = System.currentTimeMillis()
            EntryTypeStrategyRegistry.ensureRegistered()
            val dao = AppDatabase.getDatabase(appContext).vaultEntryDao()

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
                if (!scopeMatch) return@find false

                val decUser = CryptoAccess.decryptOrNull(entry.username)
                decUser == usernameValue
            }
            val matchCost = System.currentTimeMillis() - matchStart
            if (matchCost >= SLOW_ENTRY_MATCH_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry username match slow: ${matchCost}ms")
            }

            val encUser = CryptoManager.encrypt(usernameValue)
            val encPass = CryptoManager.encrypt(passwordValue)

            if (existing != null) {
                val updatedEntry = existing.copy(
                    password = encPass,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updatedEntry.toEntity())
                Logcat.i(TAG, "Updated existing account: $usernameValue")
            } else {
                val appLabel = packageName?.let { pkg ->
                    try {
                        val info = appContext.packageManager.getApplicationInfo(pkg, 0)
                        appContext.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) {
                        null
                    }
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

                dao.insert(finalizedEntry.toEntity())
                Logcat.i(TAG, "Successfully saved new entry to DB: $title")
            }

            val saveCost = System.currentTimeMillis() - saveStart
            if (saveCost >= SLOW_SAVE_FLOW_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry slow: ${saveCost}ms")
            }
            true
        } catch (e: Exception) {
            Logcat.e(TAG, "Save to database failed!", e)
            false
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

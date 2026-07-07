package com.aozijx.passly.data.repository.autofill

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.util.DomainNormalizer
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.autofill.policy.AutofillTitlePolicy
import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.AutofillMatchType
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultAutofillRepository
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import com.aozijx.passly.security.crypto.VaultLockManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutofillServiceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val sessionManager: DatabaseSessionManager,
    private val lockManager: VaultLockManager
) : VaultAutofillRepository {

    private companion object {
        private const val TAG = "AutofillRepo"
        private const val SLOW_DB_QUERY_MS = 120L
        private const val SLOW_ENTRY_MATCH_MS = 120L
        private const val SLOW_SAVE_FLOW_MS = 200L
    }

    override suspend fun updateUsageStats(entry: VaultEntry) {
        if (lockManager.isLocked()) return
        AppResult.runCatching("autofill.updateUsage") {
            val updatedEntry = entry.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = entry.usageCount + 1
            )
            sessionManager.withDatabase {
                vaultEntryDao().update(updatedEntry.toEntity())
            }
        }.onFailure {
            Logcat.e(TAG, "Failed to update usage count for ${entry.id}", it)
        }
        return
    }

    override suspend fun getEntryById(entryId: Int): VaultEntry? {
        if (lockManager.isLocked()) return null
        return AppResult.runCatching("autofill.getEntry") {
            sessionManager.withDatabase {
                vaultEntryDao().getEntryById(entryId)?.toDomain()
            }
        }.fold(
            onSuccess = { it },
            onFailure = {
                Logcat.e(TAG, "Failed to load entry by id=$entryId", it)
                null
            }
        )
    }

    override suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntry> {
        if (lockManager.isLocked()) return emptyList()
        if (entryIds.isEmpty()) return emptyList()
        return AppResult.runCatching("autofill.getEntries") {
            val order = entryIds.withIndex().associate { it.value to it.index }
            sessionManager.withDatabase {
                vaultEntryDao().getEntriesByIds(entryIds)
                    .toDomainList()
                    .sortedBy { order[it.id] ?: Int.MAX_VALUE }
            }
        }.fold(
            onSuccess = { it },
            onFailure = {
                Logcat.e(TAG, "Failed to load entries by ids", it)
                emptyList()
            }
        )
    }

    override suspend fun findMatchingCandidates(
        packageName: String?,
        webDomain: String?
    ): List<AutofillCandidate> = withContext(Dispatchers.IO) {
        if (lockManager.isLocked()) return@withContext emptyList()
        AppResult.runCatching("autofill.findCandidates") {
            if (packageName.isNullOrBlank() && webDomain.isNullOrBlank()) {
                return@runCatching emptyList()
            }

            val queryStart = System.currentTimeMillis()
            val entries = sessionManager.withDatabase {
                vaultEntryDao().getAll().toDomainList()
            }
            val queryCost = System.currentTimeMillis() - queryStart
            if (queryCost >= SLOW_DB_QUERY_MS) {
                Logcat.w(
                    TAG,
                    "findMatchingEntries query slow: ${queryCost}ms, total=${entries.size}"
                )
            }

            val normalizedPackage = normalizePackageName(packageName)
            val normalizedDomain = DomainNormalizer.normalize(webDomain)

            entries.asSequence()
                .filter { supportsAutofill(it) }
                .mapNotNull { entry ->
                    val matchType = when {
                        isPackageMatch(
                            entry.associatedAppPackage,
                            normalizedPackage
                        ) -> AutofillMatchType.APP

                        isDomainMatch(
                            entry.associatedDomain,
                            normalizedDomain
                        ) -> AutofillMatchType.DOMAIN

                        else -> return@mapNotNull null
                    }
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
        }.fold(
            onSuccess = { it },
            onFailure = {
                Logcat.e(TAG, "Failed to find matching entries", it)
                emptyList()
            }
        )
    }

    override suspend fun saveOrUpdateEntry(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (lockManager.isLocked()) return@withContext false
        AppResult.runCatching("autofill.saveOrUpdate") {
            val saveStart = System.currentTimeMillis()

            val queryStart = System.currentTimeMillis()
            val allEntries = sessionManager.withDatabase {
                vaultEntryDao().getAll().toDomainList()
            }
            val queryCost = System.currentTimeMillis() - queryStart
            if (queryCost >= SLOW_DB_QUERY_MS) {
                Logcat.w(
                    TAG,
                    "saveOrUpdateEntry query slow: ${queryCost}ms, total=${allEntries.size}"
                )
            }

            val matchStart = System.currentTimeMillis()
            val normalizedPackage = normalizePackageName(packageName)
            val normalizedDomain = DomainNormalizer.normalize(webDomain)
            val existing = allEntries.find { entry ->
                if (!supportsAutofill(entry)) return@find false

                val scopeMatch = isPackageMatch(entry.associatedAppPackage, normalizedPackage) ||
                        isDomainMatch(entry.associatedDomain, normalizedDomain)
                if (!scopeMatch) return@find false

                entry.username == usernameValue
            }
            val matchCost = System.currentTimeMillis() - matchStart
            if (matchCost >= SLOW_ENTRY_MATCH_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry username match slow: ${matchCost}ms")
            }

            if (existing != null) {
                val updatedEntry = existing.copy(
                    password = passwordValue,
                    updatedAt = System.currentTimeMillis()
                )
                sessionManager.withDatabase {
                    vaultEntryDao().update(updatedEntry.toEntity())
                }
                Logcat.i(TAG, "Updated existing account: id=${existing.id}")
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
                val fallbackCategory =
                    appContext.getString(R.string.category_autofill).ifBlank { "自动填充" }

                val newEntry = VaultEntry(
                    title = title,
                    username = usernameValue,
                    password = passwordValue,
                    category = passwordStrategy?.suggestedCategory().orEmpty()
                        .ifBlank { fallbackCategory },
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

                sessionManager.withDatabase {
                    vaultEntryDao().insert(finalizedEntry.toEntity())
                }
                Logcat.i(TAG, "Successfully saved new entry to DB: $title")
            }

            val saveCost = System.currentTimeMillis() - saveStart
            if (saveCost >= SLOW_SAVE_FLOW_MS) {
                Logcat.w(TAG, "saveOrUpdateEntry slow: ${saveCost}ms")
            }
            true
        }.fold(
            onSuccess = { it },
            onFailure = {
                Logcat.e(TAG, "Save to database failed!", it)
                false
            }
        )
    }

    private fun resolveStrategy(entryTypeValue: Int) =
        AppResult.runCatching("autofill.resolveStrategy") {
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
        val normalizedEntryDomain = DomainNormalizer.normalize(entryDomain) ?: return false
        if (normalizedEntryDomain == normalizedRequestDomain) return true
        val entryLabelCount = normalizedEntryDomain.count { it == '.' } + 1
        if (entryLabelCount < 2) return false
        return normalizedRequestDomain.endsWith(".$normalizedEntryDomain")
    }
}
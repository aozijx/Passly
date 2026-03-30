package com.example.poop.service.autofill

import android.content.Context
import com.example.poop.R
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.model.VaultEntry
import com.example.poop.types.autofill.AutofillTitleGenerator
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 自动填充数据仓库
 */
object AutofillRepository {
    private const val TAG = "AutofillRepo"

    suspend fun updateUsageStats(context: Context, entry: VaultEntry) = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val updatedEntry = entry.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = entry.usageCount + 1
            )
            dao.update(updatedEntry)
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to update usage count for ${entry.id}", e)
        }
    }

    suspend fun findMatchingEntries(context: Context, packageName: String?, webDomain: String?): List<VaultEntry> = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val entries = dao.getAllEntries().first()
            
            entries.filter { entry ->
                val pkgMatch = packageName != null && entry.associatedAppPackage == packageName
                val domainMatch = webDomain != null && entry.associatedDomain == webDomain
                pkgMatch || domainMatch
            }.sortedByDescending { it.updatedAt ?: it.createdAt ?: 0L }.take(5)
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
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val allEntries = dao.getAllEntries().first()
            
            val existing = allEntries.find { entry ->
                val scopeMatch = (entry.associatedAppPackage == packageName && packageName != null) ||
                                (entry.associatedDomain == webDomain && webDomain != null)
                if (scopeMatch) {
                    // 核心修复：直接使用简化后的解密逻辑
                    val decUser = try { CryptoManager.decrypt(entry.username) } catch (_: Exception) { null }
                    decUser == usernameValue
                } else false
            }

            val encUser = CryptoManager.encrypt(usernameValue)
            val encPass = CryptoManager.encrypt(passwordValue)
            
            if (existing != null) {
                val updatedEntry = existing.copy(
                    password = encPass,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updatedEntry)
                Logcat.i(TAG, "Updated existing account: $usernameValue")
            } else {
                val appLabel = packageName?.let { pkg ->
                    try {
                        val info = context.packageManager.getApplicationInfo(pkg, 0)
                        context.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) { null }
                }

                val title = AutofillTitleGenerator.getSmartTitle(
                    context = context,
                    pageTitle = pageTitle,
                    domain = webDomain,
                    appLabel = appLabel,
                    packageName = packageName
                )

                val newEntry = VaultEntry(
                    title = title,
                    username = encUser,
                    password = encPass,
                    category = context.getString(R.string.category_autofill),
                    associatedAppPackage = packageName,
                    associatedDomain = webDomain,
                    entryType = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.insert(newEntry)
                Logcat.i(TAG, "Saved new captured entry: $title")
            }
            return@withContext true
        } catch (e: Exception) {
            Logcat.e(TAG, "Save failed", e)
            return@withContext false
        }
    }
}

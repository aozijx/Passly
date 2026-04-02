package com.example.poop.service.autofill

import android.content.Context
import com.example.poop.R
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.model.VaultEntry
import com.example.poop.domain.model.AutofillTitleGenerator
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
            // 核心修复：直接使用 applicationContext 避免潜在的 Context 泄露
            val appContext = context.applicationContext
            val dao = AppDatabase.getDatabase(appContext).vaultDao()
            
            // 1. 先通过包名/域名匹配现有条目
            val allEntries = dao.getAllEntries().first()
            val existing = allEntries.find { entry ->
                val scopeMatch = (entry.associatedAppPackage == packageName && packageName != null) ||
                                (entry.associatedDomain == webDomain && webDomain != null)
                if (scopeMatch) {
                    val decUser = try { CryptoManager.decrypt(entry.username) } catch (_: Exception) { null }
                    decUser == usernameValue
                } else false
            }

            val encUser = CryptoManager.encrypt(usernameValue)
            val encPass = CryptoManager.encrypt(passwordValue)
            
            if (existing != null) {
                // 更新逻辑
                val updatedEntry = existing.copy(
                    password = encPass,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updatedEntry)
                Logcat.i(TAG, "Updated existing account: $usernameValue")
            } else {
                // 新建逻辑
                val appLabel = packageName?.let { pkg ->
                    try {
                        val info = appContext.packageManager.getApplicationInfo(pkg, 0)
                        appContext.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) { null }
                }

                val title = AutofillTitleGenerator.getSmartTitle(
                    context = appContext,
                    pageTitle = pageTitle,
                    domain = webDomain,
                    appLabel = appLabel,
                    packageName = packageName
                )

                // 重要：确保 category 不为空，且 entryType 设置正确
                val newEntry = VaultEntry(
                    title = title,
                    username = encUser,
                    password = encPass,
                    category = appContext.getString(R.string.category_autofill).ifBlank { "自动填充" },
                    associatedAppPackage = packageName,
                    associatedDomain = webDomain,
                    entryType = 0, // 0 表示密码类型
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // 关键点：执行插入操作
                dao.insert(newEntry)
                Logcat.i(TAG, "Successfully saved new entry to DB: $title")
            }
            return@withContext true
        } catch (e: Exception) {
            Logcat.e(TAG, "Save to database failed!", e)
            return@withContext false
        }
    }
}

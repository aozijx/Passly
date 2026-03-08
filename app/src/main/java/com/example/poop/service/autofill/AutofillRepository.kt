package com.example.poop.service.autofill

import android.content.Context
import com.example.poop.R
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.types.autofill.AutofillTitleGenerator
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 自动填充数据仓库
 * 负责处理自动填充相关的数据库更新操作与数据获取
 */
object AutofillRepository {
    private const val TAG = "AutofillRepo"

    /**
     * 异步更新条目的使用统计信息
     */
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

    /**
     * 查找与当前包名或域名匹配的条目
     */
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

    /**
     * 保存或更新自动填充捕获的条目
     */
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
            val existing = dao.getAllEntries().first().find {
                (it.associatedAppPackage == packageName && packageName != null) ||
                (it.associatedDomain == webDomain && webDomain != null)
            }

            val encUser = CryptoManager.encrypt(usernameValue, isSilent = true)
            val encPass = CryptoManager.encrypt(passwordValue, isSilent = true)
            
            if (encUser != null && encPass != null) {
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

                val entry = existing?.copy(
                    username = encUser,
                    password = encPass,
                    associatedAppPackage = packageName ?: existing.associatedAppPackage,
                    associatedDomain = webDomain ?: existing.associatedDomain,
                    updatedAt = System.currentTimeMillis()
                ) ?: VaultEntry(
                    title = title,
                    username = encUser,
                    password = encPass,
                    category = context.getString(R.string.category_autofill),
                    associatedAppPackage = packageName,
                    associatedDomain = webDomain,
                    entryType = 0,
                    updatedAt = System.currentTimeMillis()
                )

                if (existing != null) dao.update(entry) else dao.insert(entry)
                Logcat.i(TAG, "Successfully synchronized $title")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Logcat.e(TAG, "Save failed", e)
            return@withContext false
        }
    }
}

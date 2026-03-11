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
            // 获取所有条目以匹配用户名（由于加密，无法直接 SQL 查询）
            val allEntries = dao.getAllEntries().first()
            
            // 查找是否存在包名/域名匹配 且 用户名匹配的条目
            val existing = allEntries.find { entry ->
                val scopeMatch = (entry.associatedAppPackage == packageName && packageName != null) ||
                                (entry.associatedDomain == webDomain && webDomain != null)
                if (scopeMatch) {
                    // 获取 IV 并初始化解密 Cipher
                    val iv = CryptoManager.getIvFromCipherText(entry.username)
                    val cipher = iv?.let { CryptoManager.getDecryptCipher(it, isSilent = true) }
                    val decUser = cipher?.let { CryptoManager.decrypt(entry.username, it) }
                    decUser == usernameValue
                } else false
            }

            val encUser = CryptoManager.encrypt(usernameValue, isSilent = true)
            val encPass = CryptoManager.encrypt(passwordValue, isSilent = true)
            
            if (encUser != null && encPass != null) {
                if (existing != null) {
                    // 如果存在，只更新密码
                    val updatedEntry = existing.copy(
                        password = encPass,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.update(updatedEntry)
                    Logcat.i(TAG, "Updated existing account: $usernameValue")
                } else {
                    // 如果不存在，创建新条目
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
            }
            return@withContext false
        } catch (e: Exception) {
            Logcat.e(TAG, "Save failed", e)
            return@withContext false
        }
    }
}

package com.example.poop.service.autofill

import android.content.Context
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自动填充数据仓库
 * 负责处理自动填充相关的数据库更新操作
 */
object AutofillRepository {

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
            Logcat.e("AutofillRepo", "Failed to update usage count for ${entry.id}", e)
        }
    }
}

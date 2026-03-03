package com.example.poop.service.autofill

import android.content.Context
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
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
    suspend fun updateUsageStats(context: Context, item: VaultItem) = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            val updatedItem = item.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = item.usageCount + 1
            )
            dao.update(updatedItem)
        } catch (e: Exception) {
            Logcat.e("AutofillRepo", "Failed to update usage count for ${item.id}", e)
        }
    }
}
package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.Conflict
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo

/**
 * 命令 Repository：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作处理加解密和关联表的同步更新。
 *
 * 每个方法代表一个明确的命令，只接收执行该命令所需的参数。
 * 避免传递完整 [VaultEntry] 以防止 UI 层将旧快照错误地回写到数据库。
 */
interface CommandRepository {
    // ---- create ----

    suspend fun insert(entry: VaultEntry): AppResult<Long>

    // ---- metadata field commands ----

    /**
     * 更新标题。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updateTitle(id: String, expectedVersion: Int, title: String): AppResult<Unit>

    /**
     * 更新用户名（元数据中的用户名）。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updateUsername(id: String, expectedVersion: Int, username: String): AppResult<Unit>

    /**
     * 切换收藏状态。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun toggleFavorite(id: String, expectedVersion: Int): AppResult<Unit>

    /**
     * 设置图标路径。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun setIcon(id: String, expectedVersion: Int, iconPath: String?): AppResult<Unit>

    /**
     * 更新网站关联信息。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updateWebsite(
        id: String,
        expectedVersion: Int,
        website: WebsiteInfo?
    ): AppResult<Unit>

    // ---- credential field commands ----

    /**
     * 更新密码。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updatePassword(id: String, expectedVersion: Int, password: String): AppResult<Unit>

    /**
     * 更新邮箱。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updateEmail(id: String, expectedVersion: Int, email: String): AppResult<Unit>

    /**
     * 更新备注。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun updateNotes(id: String, expectedVersion: Int, notes: String): AppResult<Unit>

    // ---- lifecycle ----

    /**
     * 将条目移入回收站（软删除）。
     * @param expectedVersion 乐观锁版本，不匹配时返回 [Conflict.Failure]
     */
    suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit>

    // ---- search index ----

    /**
     * 重建所有搜索盲索引（用于存量数据迁移）。
     * @return 已重建索引的条目数量
     */
    suspend fun rebuildIndex(): AppResult<Int>
}

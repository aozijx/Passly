package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.Conflict
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 命令 Repository：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作处理加解密和关联表的同步更新。
 */
interface CommandRepository {
    suspend fun insert(entry: VaultEntry): AppResult<Long>

    /**
     * 更新条目（乐观锁）。
     *
     * @param entry 新的条目数据
     * @param expectedVersion 客户端读取时的版本号，用于冲突检测
     * @return 成功时返回 [Unit]；版本不匹配时返回 [Conflict.Failure]（需重新读取后重试）
     */
    suspend fun update(entry: VaultEntry, expectedVersion: Int): AppResult<Unit>

    suspend fun delete(entry: VaultEntry): AppResult<Unit>

    /**
     * 重建所有搜索盲索引（用于存量数据迁移）。
     * 遍历所有活跃条目，重新生成盲索引记录并替换旧数据。
     *
     * @return 已重建索引的条目数量
     */
    suspend fun rebuildIndex(): AppResult<Int>
}
package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 命令 Repository：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作处理加解密和关联表的同步更新。
 */
interface CommandRepository {
    suspend fun insert(entry: VaultEntry): AppResult<Long>
    suspend fun update(entry: VaultEntry): AppResult<Unit>
    suspend fun delete(entry: VaultEntry): AppResult<Unit>
}
package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 查询 Repository：仅负责返回数据，不涉及事务。
 * 读操作直接对接 DAO 查询，无写操作副作用。
 */
interface QueryRepository {
    suspend fun getById(entryId: String): VaultEntry?
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun count(): Int
}
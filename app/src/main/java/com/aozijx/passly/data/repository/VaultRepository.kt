package com.aozijx.passly.data.repository

import com.aozijx.passly.data.model.VaultEntry
import com.example.passly.data.local.AppPrefs
import com.example.passly.data.local.VaultDao
import com.example.passly.data.model.VaultHistory
import kotlinx.coroutines.flow.Flow

/**
 * 统一数据仓库
 */
class VaultRepository(
    private val vaultDao: VaultDao,
    private val vaultPrefs: AppPrefs
) {

    // --- 数据库操作 ---
    val allEntries: Flow<List<VaultEntry>> = vaultDao.getAllEntries()
    val allCategories: Flow<List<String>> = vaultDao.getAllCategories()

    fun getEntriesByCategory(category: String): Flow<List<VaultEntry>> = vaultDao.getEntriesByCategory(category)
    fun searchEntries(query: String): Flow<List<VaultEntry>> = vaultDao.searchEntries(query)
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> = vaultDao.getHistoryByEntryId(entryId)

    suspend fun insert(entry: VaultEntry) = vaultDao.insert(entry)
    suspend fun update(entry: VaultEntry) = vaultDao.update(entry)
    suspend fun delete(entry: VaultEntry) = vaultDao.delete(entry)
    suspend fun deleteAll() = vaultDao.deleteAll()

    // --- 偏好设置操作 ---
    val lockTimeout: Flow<Long> = vaultPrefs.lockTimeout
    val isBiometricEnabled: Flow<Boolean> = vaultPrefs.isBiometricEnabled
    val isDarkMode: Flow<Boolean?> = vaultPrefs.isDarkMode
    val isDynamicColor: Flow<Boolean> = vaultPrefs.isDynamicColor

    suspend fun setLockTimeout(timeoutMs: Long) = vaultPrefs.setLockTimeout(timeoutMs)
    suspend fun setBiometricEnabled(enabled: Boolean) = vaultPrefs.setBiometricEnabled(enabled)
    suspend fun setDarkMode(enabled: Boolean?) = vaultPrefs.setDarkMode(enabled)
    suspend fun setDynamicColor(enabled: Boolean) = vaultPrefs.setDynamicColor(enabled)
}

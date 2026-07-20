package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.VaultEntryRepository

/**
 * 图标补偿同步用例：备份导入后批量回填远程站点图标。
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconResyncUseCases @Inject constructor(
    private val vaultEntryRepository: VaultEntryRepository
) {
    suspend fun getCandidates(): List<VaultEntry> =
        vaultEntryRepository.getEntriesForIconResync()

    suspend fun update(entry: VaultEntry): AppResult<Unit> =
        vaultEntryRepository.update(entry)
}

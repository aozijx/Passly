package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetHistoryByEntryIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertHistoryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase
import kotlinx.coroutines.flow.Flow

class DetailUseCases(
    vaultRepository: VaultRepository,
    faviconRepository: FaviconRepository,
    historyRepository: HistoryRepository
) {
    private val getEntryByIdUseCase = GetEntryByIdUseCase(vaultRepository)
    private val updateEntryUseCase = UpdateEntryUseCase(vaultRepository)
    private val downloadFaviconUseCase = DownloadFaviconUseCase(faviconRepository)
    private val getHistoryByEntryIdUseCase = GetHistoryByEntryIdUseCase(historyRepository)
    private val insertHistoryUseCase = InsertHistoryUseCase(historyRepository)

    suspend fun getEntryById(entryId: Int): VaultEntry? = getEntryByIdUseCase(entryId)

    suspend fun updateEntry(entry: VaultEntry) = updateEntryUseCase(entry)

    suspend fun downloadAndApplyFavicon(entry: VaultEntry): VaultEntry? {
        val domain = entry.associatedDomain
        if (domain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return null
        val outcome = downloadFaviconUseCase(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            val updated = entry.copy(iconCustomPath = outcome.filePath)
            updateEntryUseCase(updated)
            return updated
        }
        return null
    }

    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> =
        getHistoryByEntryIdUseCase(entryId)

    suspend fun insertHistory(history: VaultHistory) = insertHistoryUseCase(history)
}
package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow

class DetailUseCases(
    private val vaultRepository: VaultRepository,
    private val faviconRepository: FaviconRepository,
    private val historyRepository: HistoryRepository
) {

    suspend fun getEntryById(entryId: Int): VaultEntry? = vaultRepository.getEntryById(entryId)

    suspend fun updateEntry(entry: VaultEntry) = vaultRepository.update(entry)

    suspend fun downloadAndApplyFavicon(entry: VaultEntry): VaultEntry? {
        val domain = entry.associatedDomain
        if (domain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return null
        val outcome = downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            val updated = entry.copy(iconCustomPath = outcome.filePath)
            vaultRepository.update(updated)
            return updated
        }
        return null
    }

    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> =
        historyRepository.getHistoryByEntryId(entryId)

    suspend fun insertHistory(history: VaultHistory) = historyRepository.insertHistory(history)

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.downloadFavicon(input)
    }
}
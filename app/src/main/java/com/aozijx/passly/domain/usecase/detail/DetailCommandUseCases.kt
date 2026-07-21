package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailCommandUseCases @Inject constructor(
    private val commandRepository: CommandRepository,
    private val faviconRepository: FaviconRepository
) {

    suspend fun updateEntry(entry: VaultEntry): AppResult<Unit> =
        commandRepository.update(entry, entry.metadata.entryVersion)

    suspend fun downloadAndApplyFavicon(entry: VaultEntry): VaultEntry? {
        val domain = entry.associatedDomain
        if (domain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return null
        val outcome = downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            val updated = entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath))
            commandRepository.update(updated, entry.metadata.entryVersion)
            return updated
        }
        return null
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }
}

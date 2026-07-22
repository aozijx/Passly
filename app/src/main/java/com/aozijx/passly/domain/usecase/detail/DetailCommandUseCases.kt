package com.aozijx.passly.domain.usecase.detail

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

    suspend fun downloadAndApplyFavicon(entry: VaultEntry): VaultEntry? {
        val domain = entry.associatedDomain
        if (domain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return null
        val outcome = downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            commandRepository.setIcon(entry.id, entry.metadata.entryVersion, outcome.filePath)
            return entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath))
        }
        return null
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }
}

package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.port.FaviconRepository

/**
 * Domain service for managing favicon-related logic across the app.
 */
class FaviconService(
    private val faviconRepository: FaviconRepository
) {
    /**
     * Downloads a favicon for the given input (URL or domain) and prepares an [EntryUpdate]
     * to apply the downloaded icon to the [Entry].
     */
    suspend fun downloadAndPrepareUpdate(
        entry: Entry,
        domain: String,
        updatePrimaryUrl: Boolean = false
    ): EntryUpdate? {
        if (domain.isBlank()) return null
        
        val outcome = faviconRepository.download(domain)
        if (outcome.result != FaviconResult.SUCCESS || outcome.filePath == null) return null

        val associations = if (updatePrimaryUrl) {
            entry.profile.associations.copy(primaryUrl = domain.trim())
        } else {
            entry.profile.associations
        }

        val updatedProfile = entry.profile.copy(
            associations = associations,
            icon = entry.profile.icon.copy(
                name = null,
                customReference = outcome.filePath,
            ),
        )

        return EntryUpdate(profile = updatedProfile)
    }

    suspend fun download(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }
}

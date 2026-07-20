package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.usecase.vault.ActivityUseCases
import com.aozijx.passly.domain.usecase.vault.SnapshotUseCases
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailUseCases @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val faviconRepository: FaviconRepository,
    private val activityUseCases: ActivityUseCases,
    private val snapshotUseCases: SnapshotUseCases
) {

    suspend fun getEntryById(entryId: String): VaultEntry? = vaultRepository.getEntryById(entryId)

    suspend fun updateEntry(entry: VaultEntry) = vaultRepository.update(entry)

    suspend fun downloadAndApplyFavicon(entry: VaultEntry): VaultEntry? {
        val domain = entry.associatedDomain
        if (domain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return null
        val outcome = downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            val updated = entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath))
            vaultRepository.update(updated)
            return updated
        }
        return null
    }

    fun getActivityByEntryId(entryId: String): Flow<List<VaultActivity>> =
        activityUseCases.observeByEntryId(entryId)

    suspend fun insertActivity(activity: VaultActivity) = activityUseCases.insert(activity)

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.downloadFavicon(input)
    }
}

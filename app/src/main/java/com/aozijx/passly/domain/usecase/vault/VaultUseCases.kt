package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.model.credential.twofactor.otp.OtpConfig
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.LookupRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultUseCases @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val lookupRepository: LookupRepository,
    private val otpRepository: OtpRepository,
    private val faviconRepository: FaviconRepository
) {

    fun observeEntrySummaries(
        query: String, category: String?, filter: LookupRepository.EntryFilter
    ): Flow<List<VaultEntry>> =
        lookupRepository.observeEntrySummariesByDemand(query, category, filter)

    fun observeCategoriesByFilter(
        filter: LookupRepository.EntryFilter
    ): Flow<List<String>> = lookupRepository.getCategoriesByFilter(filter)

    suspend fun getEntryById(entryId: String): VaultEntry? = vaultRepository.getEntryById(entryId)

    suspend fun addEntry(entry: VaultEntry, domain: String? = null): AppResult<Long> {
        val insertResult = vaultRepository.insert(entry)
        if (insertResult is AppResult.Success && !domain.isNullOrBlank()) {
            val outcome = downloadFavicon(domain)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                vaultRepository.update(
                    entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath))
                )
            }
        }
        return insertResult
    }

    suspend fun updateEntry(entry: VaultEntry): AppResult<Unit> = vaultRepository.update(entry)

    suspend fun deleteEntry(entry: VaultEntry): AppResult<Unit> = vaultRepository.delete(entry)

    suspend fun recordUsage(entryId: String): AppResult<Unit> = vaultRepository.recordUsage(entryId)

    fun getTotpCode(config: OtpConfig): String = otpRepository.generateTotp(config)

    suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.downloadFavicon(input)
    }

    suspend fun downloadMissingFavicons(summaries: List<VaultEntry>) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                val domain = summary.associatedDomain ?: return@forEach
                val outcome = downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    vaultRepository.getEntryById(summary.id)?.let { entry ->
                        vaultRepository.update(entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath)))
                    }
                }
            }
    }
}

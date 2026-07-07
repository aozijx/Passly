package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultUseCases @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val vaultSearchRepository: VaultSearchRepository,
    private val otpRepository: OtpRepository,
    private val faviconRepository: FaviconRepository
) {

    fun observeEntrySummaries(
        query: String, category: String?, filter: VaultSearchRepository.EntryFilter
    ): Flow<List<VaultSummary>> =
        vaultSearchRepository.observeEntrySummariesByDemand(query, category, filter)

    fun observeCategoriesByFilter(
        filter: VaultSearchRepository.EntryFilter
    ): Flow<List<String>> = vaultSearchRepository.getCategoriesByFilter(filter)

    suspend fun getEntryById(entryId: Int): VaultEntry? = vaultRepository.getEntryById(entryId)

    suspend fun addEntry(entry: VaultEntry, domain: String? = null): AppResult<Long> {
        val insertResult = vaultRepository.insert(entry)
        if (insertResult is AppResult.Success && !domain.isNullOrBlank()) {
            val id = insertResult.data
            val outcome = downloadFavicon(domain)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                vaultRepository.update(
                    entry.copy(id = id.toInt(), iconName = null, iconCustomPath = outcome.filePath)
                )
            }
        }
        return insertResult
    }

    suspend fun updateEntry(entry: VaultEntry): AppResult<Unit> = vaultRepository.update(entry)

    suspend fun deleteEntry(entry: VaultEntry): AppResult<Unit> = vaultRepository.delete(entry)

    suspend fun recordUsage(entryId: Int): AppResult<Unit> = vaultRepository.recordUsage(entryId)

    fun getTotpCode(config: TotpConfig): String = otpRepository.generateTotp(config)

    suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.downloadFavicon(input)
    }

    fun decryptSingleWithAuth(
        launcher: BiometricPromptLauncher,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: (BiometricPromptLauncher, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) {
            onResult("")
            return
        }
        authenticate(launcher, promptTitle, promptSubtitle, null) {
            onResult(encryptedData)
        }
    }

    suspend fun downloadMissingFavicons(summaries: List<VaultSummary>) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                val domain = summary.associatedDomain ?: return@forEach
                val outcome = downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    vaultRepository.getEntryById(summary.id)?.let { entry ->
                        vaultRepository.update(entry.copy(iconCustomPath = outcome.filePath))
                    }
                }
            }
    }
}
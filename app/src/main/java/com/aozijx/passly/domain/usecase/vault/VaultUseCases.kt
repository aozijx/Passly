package com.aozijx.passly.domain.usecase.vault

import androidx.fragment.app.FragmentActivity
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

    suspend fun addEntry(entry: VaultEntry, domain: String? = null): Long {
        val id = vaultRepository.insert(entry)
        if (!domain.isNullOrBlank()) {
            val outcome = downloadFavicon(domain)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                vaultRepository.update(
                    entry.copy(id = id.toInt(), iconName = null, iconCustomPath = outcome.filePath)
                )
            }
        }
        return id
    }

    suspend fun updateEntry(entry: VaultEntry) = vaultRepository.update(entry)

    suspend fun deleteEntry(entry: VaultEntry) = vaultRepository.delete(entry)

    suspend fun recordUsage(entryId: Int) = vaultRepository.recordUsage(entryId)

    fun getTotpCode(config: TotpConfig): String = otpRepository.generateTotp(config)

    fun getTotpCode(
        secret: String, digits: Int, period: Int, algorithm: String
    ): String = getTotpCode(
        TotpConfig(
            secret = secret,
            digits = digits,
            period = period,
            algorithm = algorithm
        )
    )

    suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.downloadFavicon(input)
    }

    fun decryptSingleWithAuth(
        activity: FragmentActivity,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) {
            onResult("")
            return
        }
        authenticate(activity, promptTitle, promptSubtitle, null) {
            onResult(encryptedData)
        }
    }

    suspend fun downloadMissingFavicons(summaries: List<VaultSummary>) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                val outcome = downloadFavicon(summary.associatedDomain!!)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    vaultRepository.getEntryById(summary.id)?.let { entry ->
                        vaultRepository.update(entry.copy(iconCustomPath = outcome.filePath))
                    }
                }
            }
    }
}
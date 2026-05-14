package com.aozijx.passly.domain.usecase.vault

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.icon.FaviconOutcome
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.impl.DecryptSingleWithAuthUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DeleteEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.DownloadFaviconUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.vault.impl.GetTotpCodeUseCase
import com.aozijx.passly.domain.usecase.vault.impl.InsertEntryUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveCategoriesByFilterUseCase
import com.aozijx.passly.domain.usecase.vault.impl.ObserveEntrySummariesByDemandUseCase
import com.aozijx.passly.domain.usecase.vault.impl.RecordUsageUseCase
import com.aozijx.passly.domain.usecase.vault.impl.UpdateEntryUseCase
import kotlinx.coroutines.flow.Flow

class VaultUseCases(
    vaultRepository: VaultRepository,
    vaultSearchRepository: VaultSearchRepository,
    otpRepository: OtpRepository,
    faviconRepository: FaviconRepository
) {
    private val insertEntryUseCase = InsertEntryUseCase(vaultRepository)
    private val deleteEntryUseCase = DeleteEntryUseCase(vaultRepository)
    private val updateEntryUseCase = UpdateEntryUseCase(vaultRepository)
    private val getEntryByIdUseCase = GetEntryByIdUseCase(vaultRepository)
    private val recordUsageUseCase = RecordUsageUseCase(vaultRepository)
    private val observeSummaries = ObserveEntrySummariesByDemandUseCase(vaultSearchRepository)
    private val observeCategories = ObserveCategoriesByFilterUseCase(vaultSearchRepository)
    private val downloadFaviconUseCase = DownloadFaviconUseCase(faviconRepository)
    private val getTotpCodeUseCase = GetTotpCodeUseCase(otpRepository)
    private val decryptSingleUseCase = DecryptSingleWithAuthUseCase()

    fun observeEntrySummaries(
        query: String, category: String?, filter: VaultSearchRepository.EntryFilter
    ): Flow<List<VaultSummary>> = observeSummaries(query, category, filter)

    fun observeCategoriesByFilter(
        filter: VaultSearchRepository.EntryFilter
    ): Flow<List<String>> = observeCategories(filter)

    suspend fun getEntryById(entryId: Int): VaultEntry? = getEntryByIdUseCase(entryId)

    suspend fun addEntry(entry: VaultEntry, domain: String? = null): Long {
        val id = insertEntryUseCase(entry)
        if (!domain.isNullOrBlank()) {
            val outcome = downloadFaviconUseCase(domain)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                updateEntryUseCase(
                    entry.copy(id = id.toInt(), iconName = null, iconCustomPath = outcome.filePath)
                )
            }
        }
        return id
    }

    suspend fun updateEntry(entry: VaultEntry) = updateEntryUseCase(entry)

    suspend fun deleteEntry(entry: VaultEntry) = deleteEntryUseCase(entry)

    suspend fun recordUsage(entryId: Int) = recordUsageUseCase(entryId)

    fun getTotpCode(config: TotpConfig): String = getTotpCodeUseCase(config)

    suspend fun downloadFavicon(domain: String): FaviconOutcome = downloadFaviconUseCase(domain)

    fun decryptSingleWithAuth(
        activity: FragmentActivity,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) = decryptSingleUseCase(
        activity,
        encryptedData,
        promptTitle,
        promptSubtitle,
        authenticate,
        onResult
    )

    suspend fun downloadMissingFavicons(summaries: List<VaultSummary>) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                val outcome = downloadFaviconUseCase(summary.associatedDomain!!)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    getEntryByIdUseCase(summary.id)?.let { entry ->
                        updateEntryUseCase(entry.copy(iconCustomPath = outcome.filePath))
                    }
                }
            }
    }
}
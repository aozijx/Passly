package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.core.security.otp.TwoFAUtils
import com.aozijx.passly.domain.model.FaviconOutcome
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.FaviconRepository
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

// ============================================================================
// OBSERVE & QUERY USE CASES
// ============================================================================

class ObserveAllEntriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<VaultEntry>> = repository.allEntries
}

class ObserveAllEntrySummariesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<VaultSummary>> = repository.allEntrySummaries
}

class GetEntriesByCategoryUseCase(private val repository: VaultRepository) {
    operator fun invoke(category: String): Flow<List<VaultEntry>> = repository.getEntriesByCategory(category)
}

class GetEntrySummariesByCategoryUseCase(private val repository: VaultRepository) {
    operator fun invoke(category: String): Flow<List<VaultSummary>> = repository.getEntrySummariesByCategory(category)
}

class SearchEntriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(query: String): Flow<List<VaultEntry>> = repository.searchEntries(query)
}

class SearchEntrySummariesUseCase(private val repository: VaultRepository) {
    operator fun invoke(query: String): Flow<List<VaultSummary>> = repository.searchEntrySummaries(query)
}

class GetCategoriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<String>> = repository.allCategories
}

class GetHistoryByEntryIdUseCase(private val repository: VaultRepository) {
    operator fun invoke(entryId: Int): Flow<List<VaultHistory>> = repository.getHistoryByEntryId(entryId)
}

class GetEntryByIdUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entryId: Int): VaultEntry? = repository.getEntryById(entryId)
}

// ============================================================================
// WRITE USE CASES
// ============================================================================

class InsertEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry): Long = repository.insert(entry)
}

class UpdateEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry) = repository.update(entry)
}

class DeleteEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry) = repository.delete(entry)
}

class DeleteAllEntriesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke() = repository.deleteAll()
}

// ============================================================================
// UTILITY USE CASES
// ============================================================================

class GetTotpCodeUseCase {
    operator fun invoke(config: TotpConfig): String {
        val normalizedDigits = if (config.algorithm.uppercase() == "STEAM") 5 else config.digits
        return TwoFAUtils.generateTotp(config.secret, normalizedDigits, config.period, config.algorithm)
    }

    operator fun invoke(
        secret: String,
        digits: Int,
        period: Int,
        algorithm: String
    ): String = invoke(TotpConfig(secret = secret, digits = digits, period = period, algorithm = algorithm))
}

class DownloadFaviconUseCase(
    private val repository: FaviconRepository
) {
    suspend operator fun invoke(input: String): FaviconOutcome {
        return repository.downloadFavicon(input)
    }
}

package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.aozijx.passly.domain.repository.entry.QueryRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultCommandUseCases @Inject constructor(
    private val commandRepository: CommandRepository,
    private val queryRepository: QueryRepository,
    private val faviconRepository: FaviconRepository
) {

    suspend fun addEntry(entry: VaultEntry, domain: String? = null): AppResult<Long> {
        val insertResult = commandRepository.insert(entry)
        if (insertResult is AppResult.Success && !domain.isNullOrBlank()) {
            val outcome = downloadFavicon(domain)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                // 插入后重新读取以获取正确的 entryVersion，避免乐观锁冲突
                val savedEntry = queryRepository.getById(entry.id)
                if (savedEntry != null) {
                    commandRepository.update(
                        savedEntry.copy(metadata = savedEntry.metadata.copy(icon = outcome.filePath)),
                        savedEntry.metadata.entryVersion
                    )
                }
            }
        }
        return insertResult
    }

    suspend fun updateEntry(entry: VaultEntry): AppResult<Unit> =
        commandRepository.update(entry, entry.metadata.entryVersion)

    suspend fun deleteEntry(entry: VaultEntry): AppResult<Unit> = commandRepository.delete(entry)

    suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }

    suspend fun downloadMissingFavicons(summaries: List<VaultEntry>) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                val domain = summary.associatedDomain ?: return@forEach
                val outcome = downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    queryRepository.getById(summary.id)?.let { entry ->
                        commandRepository.update(
                            entry.copy(metadata = entry.metadata.copy(icon = outcome.filePath)),
                            entry.metadata.entryVersion
                        )
                    }
                }
            }
    }
}

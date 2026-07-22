package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo
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
                    commandRepository.setIcon(
                        savedEntry.id,
                        savedEntry.metadata.entryVersion,
                        outcome.filePath
                    )
                }
            }
        }
        return insertResult
    }

    suspend fun updateTitle(id: String, expectedVersion: Int, title: String): AppResult<Unit> =
        commandRepository.updateTitle(id, expectedVersion, title)

    suspend fun updateUsername(
        id: String,
        expectedVersion: Int,
        username: String
    ): AppResult<Unit> =
        commandRepository.updateUsername(id, expectedVersion, username)

    suspend fun toggleFavorite(id: String, expectedVersion: Int): AppResult<Unit> =
        commandRepository.toggleFavorite(id, expectedVersion)

    suspend fun setIcon(id: String, expectedVersion: Int, iconPath: String?): AppResult<Unit> =
        commandRepository.setIcon(id, expectedVersion, iconPath)

    suspend fun updateWebsite(
        id: String,
        expectedVersion: Int,
        website: WebsiteInfo?
    ): AppResult<Unit> =
        commandRepository.updateWebsite(id, expectedVersion, website)

    suspend fun updatePassword(
        id: String,
        expectedVersion: Int,
        password: String
    ): AppResult<Unit> =
        commandRepository.updatePassword(id, expectedVersion, password)

    suspend fun updateEmail(id: String, expectedVersion: Int, email: String): AppResult<Unit> =
        commandRepository.updateEmail(id, expectedVersion, email)

    suspend fun updateNotes(id: String, expectedVersion: Int, notes: String): AppResult<Unit> =
        commandRepository.updateNotes(id, expectedVersion, notes)

    suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit> =
        commandRepository.moveToTrash(id, expectedVersion)

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
                        commandRepository.setIcon(
                            entry.id,
                            entry.metadata.entryVersion,
                            outcome.filePath
                        )
                    }
                }
            }
    }
}

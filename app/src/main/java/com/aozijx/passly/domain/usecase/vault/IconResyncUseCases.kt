package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.aozijx.passly.domain.repository.entry.QueryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconResyncUseCases @Inject constructor(
    private val queryRepository: QueryRepository,
    private val commandRepository: CommandRepository
) {
    suspend fun getCandidates(): List<VaultEntry> =
        queryRepository.getEntriesForIconResync()

    suspend fun setIcon(entry: VaultEntry): AppResult<Unit> =
        commandRepository.setIcon(entry.id, entry.metadata.entryVersion, entry.metadata.icon)
}

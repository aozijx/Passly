package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DetailEntryUpdateCoordinator(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
) {
    private val updateMutex = Mutex()

    suspend fun update(
        entryId: EntryId,
        patch: DetailEntryPatch,
    ): AppResult<Entry> = updateMutex.withLock {
        var conflictRetries = 0
        while (true) {
            val latest = entryQueryRepository.getById(entryId)
                ?: return@withLock AppResult.Failure(NotFound())
            val updated = patch.applyTo(latest)
            when (
                val result = entryCommandRepository.updateEntry(
                    id = entryId,
                    expectedVersion = latest.version,
                    changes = EntryUpdate(
                        profile = updated.profile,
                        secret = updated.secret,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val persisted = entryQueryRepository.getById(entryId)
                        ?: return@withLock AppResult.Failure(NotFound())
                    return@withLock AppResult.Success(persisted)
                }

                is AppResult.Failure -> {
                    if (result.error is Conflict && conflictRetries == 0) {
                        conflictRetries += 1
                        continue
                    }
                    return@withLock result
                }
            }
        }
        error("Unreachable detail entry update state")
    }
}

package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import javax.inject.Inject

internal data class DetailEntrySnapshot(
    val entry: Entry,
    val sensitiveFieldKeys: Set<SensitiveFieldKey>,
)

internal class DetailEntryLoader @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
) {
    suspend fun load(entryId: EntryId): DetailEntrySnapshot? {
        val entry = entryQueryRepository.getById(entryId) ?: return null
        val presence = sensitiveFieldRepository.getPresence(entry.id)
        return DetailEntrySnapshot(
            entry = entry,
            sensitiveFieldKeys = presence.keys,
        )
    }
}

package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.entry.VaultEntry
import kotlinx.coroutines.flow.Flow

interface LookupRepository {

    enum class EntryFilter {
        ALL, TOTP_ONLY, PASSWORD_ONLY
    }

    val allCategories: Flow<List<String>>
    fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<VaultEntry>>

    fun getCategoriesByFilter(filter: EntryFilter): Flow<List<String>>
}

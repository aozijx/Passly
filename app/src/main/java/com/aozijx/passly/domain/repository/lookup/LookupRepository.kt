package com.aozijx.passly.domain.repository.lookup

import com.aozijx.passly.domain.model.entry.VaultEntry
import kotlinx.coroutines.flow.Flow

interface LookupRepository {

    enum class EntryFilter {
        ALL, TOTP_ONLY, PASSWORD_ONLY
    }

    val allCategories: Flow<List<String>>
    fun observe(query: String, category: String?, filter: EntryFilter): Flow<List<VaultEntry>>
    fun observeCategories(filter: EntryFilter): Flow<List<String>>
}

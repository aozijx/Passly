package com.aozijx.passly.domain.repository.lookup

import com.aozijx.passly.domain.model.lookup.VaultListItem
import kotlinx.coroutines.flow.Flow

interface LookupRepository {

    enum class EntryFilter {
        ALL, TOTP_ONLY, PASSWORD_ONLY
    }

    val allCategories: Flow<List<String>>
    fun observe(query: String, category: String?, filter: EntryFilter): Flow<List<VaultListItem>>
    fun observeCategories(filter: EntryFilter): Flow<List<String>>
}

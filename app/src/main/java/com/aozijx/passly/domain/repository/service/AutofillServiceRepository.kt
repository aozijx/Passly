package com.aozijx.passly.domain.repository.service

import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.VaultEntry

interface AutofillServiceRepository {
    suspend fun updateUsageStats(entry: VaultEntry)
    suspend fun getEntryById(entryId: Int): VaultEntry?
    suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntry>
    suspend fun findMatchingCandidates(packageName: String?, webDomain: String?): List<AutofillCandidate>
    suspend fun saveOrUpdateEntry(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}

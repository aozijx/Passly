package com.aozijx.passly.domain.usecase.autofill

import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class AutofillUseCases(private val repository: AutofillServiceRepository) {

    suspend fun updateUsageStats(entry: VaultEntry) = repository.updateUsageStats(entry)

    suspend fun getEntryById(entryId: Int): VaultEntry? = repository.getEntryById(entryId)

    suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntry> =
        repository.getEntriesByIds(entryIds)

    suspend fun findMatchingCandidates(
        packageName: String?, webDomain: String?
    ): List<AutofillCandidate> = repository.findMatchingCandidates(packageName, webDomain)

    suspend fun saveOrUpdateEntry(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = repository.saveOrUpdateEntry(
        packageName,
        webDomain,
        pageTitle,
        usernameValue,
        passwordValue
    )
}
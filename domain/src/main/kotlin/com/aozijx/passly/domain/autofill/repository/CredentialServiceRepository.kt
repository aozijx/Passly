package com.aozijx.passly.domain.autofill.repository

import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate

interface CredentialServiceRepository {
    suspend fun search(
        packageName: String?,
        webDomain: String?,
        allowUnmatched: Boolean,
        includeSecrets: Boolean,
        limit: Int,
    ): List<CredentialCandidate>

    suspend fun getById(entryId: String): EntryAggregate?
    suspend fun getByIds(
        entryIds: List<String>,
        includeSecrets: Boolean = true
    ): List<EntryAggregate>
    suspend fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}

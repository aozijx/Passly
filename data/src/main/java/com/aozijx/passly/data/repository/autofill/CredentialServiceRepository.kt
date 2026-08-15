package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate

interface CredentialServiceRepository {
    suspend fun search(
        packageName: String?,
        webDomain: String?,
        allowUnmatched: Boolean,
        includeSecrets: Boolean,
        limit: Int,
    ): List<CredentialCandidate>

    suspend fun getById(entryId: String): Entry?
    suspend fun getByIds(
        entryIds: List<String>,
        includeSecrets: Boolean = true
    ): List<Entry>
    suspend fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}

package com.aozijx.passly.domain.autofill.repository

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate

interface CredentialServiceRepository {
    suspend fun search(
        packageName: String?,
        webDomain: String?,
        allowUnmatched: Boolean,
        includeSecrets: Boolean,
        limit: Int,
    ): List<CredentialCandidate>

    suspend fun getById(entryId: String): VaultEntry?
    suspend fun getByIds(
        entryIds: List<String>,
        includeSecrets: Boolean = true
    ): List<VaultEntry>
    suspend fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}

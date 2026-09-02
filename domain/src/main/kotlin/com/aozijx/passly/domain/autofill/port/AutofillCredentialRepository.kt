package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate

interface AutofillCredentialRepository {
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
        includeSecrets: Boolean = true,
    ): List<Entry>
}

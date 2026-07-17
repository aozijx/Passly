package com.aozijx.passly.domain.repository.autofill

import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.CredentialCandidate

interface CredentialServiceRepository {
    fun search(packageName: String?, webDomain: String?): List<CredentialCandidate>
    fun getById(entryId: Int): VaultEntry?
    fun getByIds(entryIds: List<Int>): List<VaultEntry>
    fun updateLastUsed(entryId: Int)
    fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}
package com.aozijx.passly.domain.autofill.repository

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate

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

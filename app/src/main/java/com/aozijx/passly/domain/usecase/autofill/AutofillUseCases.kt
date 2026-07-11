package com.aozijx.passly.domain.usecase.autofill

import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.CredentialRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutofillUseCases @Inject constructor(private val repository: CredentialRepository) {

    fun search(
        packageName: String?, webDomain: String?
    ): List<CredentialCandidate> = repository.search(packageName, webDomain)

    fun getById(entryId: Int): VaultEntry? = repository.getById(entryId)

    fun getByIds(entryIds: List<Int>): List<VaultEntry> = repository.getByIds(entryIds)

    fun updateLastUsed(entryId: Int) = repository.updateLastUsed(entryId)

    fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = repository.save(
        packageName,
        webDomain,
        pageTitle,
        usernameValue,
        passwordValue
    )
}

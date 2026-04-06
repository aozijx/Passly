package com.aozijx.passly.domain.usecase.autofill.impl

import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class SaveOrUpdateEntryUseCase(private val repository: AutofillServiceRepository) {
    suspend operator fun invoke(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = repository.saveOrUpdateEntry(packageName, webDomain, pageTitle, usernameValue, passwordValue)
}

package com.aozijx.passly.domain.autofill.model

data class AutofillSaveDraft(
    val packageName: String,
    val webDomain: String?,
    val pageTitle: String?,
    val username: String,
    val password: String,
)

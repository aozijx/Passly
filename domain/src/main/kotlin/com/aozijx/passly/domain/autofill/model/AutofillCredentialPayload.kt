package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.EntryId

data class AutofillCredentialSelection(
    val entryId: EntryId,
    val requestedFields: Set<AutofillCredentialField>,
    val scope: AutofillGrantContext,
    val strictDomainMatching: Boolean,
)

enum class AutofillCredentialField {
    USERNAME,
    PASSWORD,
    OTP,
}

data class AutofillCredentialPayload(
    val entryId: EntryId,
    val fields: Map<AutofillCredentialField, String>,
) {
    init {
        require(fields.isNotEmpty())
    }
}

sealed interface AutofillCredentialResolution {
    data class Resolved(
        val payload: AutofillCredentialPayload,
    ) : AutofillCredentialResolution

    data object NotFound : AutofillCredentialResolution

    data object NotFillable : AutofillCredentialResolution
}

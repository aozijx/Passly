package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.EntryId

data class AutofillCredentialSelection(
    val entryId: EntryId,
    val includeOtp: Boolean,
)

data class AutofillCredentialPayload(
    val entryId: EntryId,
    val username: String,
    val password: String,
    val otpCode: String? = null,
)

sealed interface AutofillCredentialResolution {
    data class Resolved(val payload: AutofillCredentialPayload) : AutofillCredentialResolution
    data object NotFound : AutofillCredentialResolution
    data object NotFillable : AutofillCredentialResolution
}

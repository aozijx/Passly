package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection

interface AutofillCredentialResolver {
    suspend fun resolve(selection: AutofillCredentialSelection): AutofillCredentialResolution
}

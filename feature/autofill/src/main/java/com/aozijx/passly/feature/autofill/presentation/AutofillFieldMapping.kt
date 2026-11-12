package com.aozijx.passly.feature.autofill.presentation

import com.aozijx.passly.domain.autofill.model.AutofillCredentialField
import com.aozijx.passly.domain.autofill.model.FieldRole

internal fun Map<String, FieldRole>.requestedCredentialFields(): Set<AutofillCredentialField> =
    buildSet {
        if (FieldRole.USERNAME in values) add(AutofillCredentialField.USERNAME)
        if (FieldRole.PASSWORD in values) add(AutofillCredentialField.PASSWORD)
        if (FieldRole.OTP in values) add(AutofillCredentialField.OTP)
    }

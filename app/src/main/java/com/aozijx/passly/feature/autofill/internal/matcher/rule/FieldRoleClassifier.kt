package com.aozijx.passly.feature.autofill.internal.matcher.rule

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.FieldRole

/**
 * Interface for individual classification rules that identify the role of an autofill field.
 */
interface FieldRoleClassifier {
    fun classify(field: AutofillField): FieldRole
}

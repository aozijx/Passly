package com.aozijx.passly.feature.autofill.legacy.service.mapping

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

internal fun ParsedStructure.toAutofillRequest(): AutofillRequest =
    AutofillRequest(
        packageName = packageName.orEmpty(),
        domain = webDomain,
        fields =
            editableFields.map { field ->
                AutofillField(
                    id = field.autofillId.toString(),
                    hints = field.autofillHints.toSet(),
                    inputType = field.inputType,
                    isFocused = field.isFocused,
                    resourceId = field.resourceId,
                    contentDescription = field.contentDescription,
                    className = field.className,
                    text = field.value,
                    hint = field.hint,
                )
            },
        source = AutofillSource.AUTOFILL_SERVICE,
        ambiguousWebOrigin = ambiguousWebOrigin,
    )

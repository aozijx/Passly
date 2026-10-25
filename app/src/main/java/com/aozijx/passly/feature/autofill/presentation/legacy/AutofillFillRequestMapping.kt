package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.feature.autofill.legacy.service.parser.EditableFieldInfo
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure

internal fun AutofillFillRequest.roleMap(): Map<String, FieldRole> = buildMap {
    usernameIds.forEach { put(it.toString(), FieldRole.USERNAME) }
    passwordIds.forEach { put(it.toString(), FieldRole.PASSWORD) }
    otpIds.forEach { put(it.toString(), FieldRole.OTP) }
}

internal fun AutofillFillRequest.toDomainRequest(): AutofillRequest {
    val roles = roleMap()
    return AutofillRequest(
        packageName = packageName,
        domain = webDomain,
        fields = editableIds.map { id ->
            AutofillField(
                id = id.toString(),
                hints = emptySet(),
                inputType = null,
                isFocused = false,
                role = roles[id.toString()],
            )
        },
        source = AutofillSource.AUTOFILL_SERVICE,
    )
}

internal fun AutofillFillRequest.toParsedStructure(): ParsedStructure = ParsedStructure(
    packageName = packageName,
    webDomain = webDomain,
    editableFields = editableIds.map(::EditableFieldInfo),
)

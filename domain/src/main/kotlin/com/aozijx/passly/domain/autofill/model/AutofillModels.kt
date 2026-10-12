package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.autofill.AutofillScope

/**
 * Field roles: identifies the type of an autofill field.
 */
enum class FieldRole {
    PASSWORD,
    USERNAME,
    OTP,
    SUBMIT,
    UNKNOWN,
}

/**
 * Source of the fill request.
 */
enum class AutofillSource {
    AUTOFILL_SERVICE,
    CREDENTIAL_MANAGER,
}

/**
 * Represents a single field on a page for autofill processing.
 */
data class AutofillField(
    val id: String,
    val hints: Set<String>,
    val inputType: String?,
    val isFocused: Boolean,
    val text: String? = null,
    val hint: String? = null,
    val role: FieldRole? = null,
    val resourceId: String? = null,
    val contentDescription: String? = null,
    val className: String? = null
)

/**
 * Platform-agnostic autofill request.
 */
data class AutofillRequest(
    val packageName: String,
    val domain: String?,
    val fields: List<AutofillField>,
    val source: AutofillSource,
    val activityTitle: String? = null,
    val isInlineRequest: Boolean = false
)

/**
 * Normalized calling-app and web-origin scope owned by an Autofill interaction.
 */
data class AutofillGrantContext(
    val packageName: String,
    val webDomain: String? = null,
) {
    fun normalized(): AutofillGrantContext = copy(
        packageName = packageName.trim().lowercase(),
        webDomain = AutofillScope.normalizeDomain(webDomain)
    )
}

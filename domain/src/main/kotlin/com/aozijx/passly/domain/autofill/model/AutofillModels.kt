package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.settings.model.AutofillPresentation

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
 * Status of the fill response.
 */
enum class AutofillStatus {
    READY,
    LOCKED,
    DISABLED,
    NO_MATCH,
    UNSUPPORTED_FIELDS,
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
 * Platform-agnostic autofill response.
 */
data class AutofillResponse(
    val candidates: List<ResolvedCandidate> = emptyList(),
    val status: AutofillStatus = AutofillStatus.READY,
    val roleMap: Map<String, FieldRole> = emptyMap(),
    val requireAuthentication: Boolean = true,
    val savePromptsEnabled: Boolean = false,
    val presentation: AutofillPresentation = AutofillPresentation.SYSTEM_INLINE
)

/**
 * Autofill grant context: used for scope matching for short-term session authorization.
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

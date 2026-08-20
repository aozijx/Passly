package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.FieldRole

/**
 * Provides keywords and regex patterns for heuristic field matching.
 * This allows externalizing matching logic to resource files for localization and easier updates.
 */
interface AutofillHintProvider {
    fun getUsernamePattern(): Regex
    fun getPasswordPattern(): Regex
    fun getOtpPattern(): Regex
    fun getSubmitPattern(): Regex
    fun getSearchPattern(): Regex
    fun getConfirmationPattern(): Regex

    /**
     * Maps standard platform hints (e.g., "username", "password") to internal FieldRole.
     */
    fun getHintRoleMap(): Map<String, FieldRole>
}

package com.aozijx.passly.data.settings.model

/**
 * Traditional AutofillService and Credential Manager share this policy.
 *
 * System registration is still controlled by Android. [enabled] and
 * [credentialManagerEnabled] only control whether Passly returns suggestions.
 */
data class AutofillSettings(
    val enabled: Boolean = true,
    val presentation: AutofillPresentation = AutofillPresentation.SYSTEM_INLINE,
    val credentialManagerEnabled: Boolean = true,
    val requireAuthentication: Boolean = true,
    val includeOtp: Boolean = true,
    val savePromptsEnabled: Boolean = true,
    val allowUnmatchedSuggestions: Boolean = false,
    val maxSuggestions: Int = DEFAULT_MAX_SUGGESTIONS,
) {
    val normalizedMaxSuggestions: Int
        get() = maxSuggestions.coerceIn(MIN_SUGGESTIONS, MAX_SUGGESTIONS)

    companion object {
        const val DEFAULT_MAX_SUGGESTIONS = 5
        const val MIN_SUGGESTIONS = 1
        const val MAX_SUGGESTIONS = 10
    }
}

enum class AutofillPresentation {
    SYSTEM_INLINE,
    BOTTOM_SHEET,
}

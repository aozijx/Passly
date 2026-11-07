package com.aozijx.passly.domain.settings.model

/**
 * Autofill entry points share discovery policy where their platform contracts allow it.
 *
 * System registration is still controlled by Android. [enabled] is the global
 * Passly Autofill gate; [credentialManagerEnabled] adds a Credential Manager
 * specific gate.
 * [presentation] applies to the traditional AutofillService; Credential Manager
 * and Accessibility use their platform-specific presentation flows. OTP is
 * included only when the current request exposes an OTP field; Credential
 * Manager password requests do not support OTP credentials.
 */
data class AutofillSettings(
    val enabled: Boolean = true,
    val presentation: AutofillPresentation = AutofillPresentation.SYSTEM_INLINE,
    val credentialManagerEnabled: Boolean = true,
    val requireAuthentication: Boolean = true,
    val strictDomainMatching: Boolean = true,
)

enum class AutofillPresentation(
    val wireKey: String,
) {
    FLOATING("floating"),
    SYSTEM_INLINE("system_inline"),
    BOTTOM_SHEET("bottom_sheet"),
    ;

    companion object {
        fun fromWireKey(value: String?): AutofillPresentation? = entries.firstOrNull { it.wireKey == value }
    }
}

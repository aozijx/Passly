package com.aozijx.passly.feature.vault.otp.presentation

import androidx.compose.runtime.Immutable
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** Presentation capability passed from the Vault item projection to OTP rendering. */
@Immutable
sealed interface OtpPresentation {
    data object NotApplicable : OtpPresentation

    data class Applicable(
        val type: OtpType,
    ) : OtpPresentation
}

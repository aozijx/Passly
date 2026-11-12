package com.aozijx.passly.feature.vault.otp.presentation

import com.aozijx.passly.feature.vault.otp.OtpCodeState

internal fun OtpCodeState?.toOtpUiState(
    presentation: OtpPresentation,
    showCode: Boolean,
): OtpUiState {
    val type = (presentation as? OtpPresentation.Applicable)?.type ?: return OtpUiState.NotApplicable
    if (!showCode) return OtpUiState.Hidden
    return this?.toUiModel(type.plugin().formatCode) ?: OtpUiState.NotRequested
}

private fun OtpCodeState.toUiModel(formatter: (String) -> String): OtpUiState =
    when (this) {
        OtpCodeState.Pending -> OtpUiState.Pending
        is OtpCodeState.TimedReady ->
            OtpUiState.TimedReady(
                currentCode = formatter(code),
                nextCode = formatter(nextCode),
                windowEndMillis = windowEndMillis,
                periodSeconds = periodSeconds,
            )
        is OtpCodeState.HotpReady ->
            OtpUiState.HotpReady(
                currentCode = formatter(code),
                counter = counter,
                canRollbackPrevious = canRollbackPrevious,
            )
        is OtpCodeState.Failed -> OtpUiState.Failed(canRollbackPrevious)
}

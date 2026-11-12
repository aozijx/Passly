package com.aozijx.passly.feature.vault.otp.presentation

import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.OtpCodeState

internal fun OtpCodeState?.toOtpUiState(
    presentation: OtpPresentation,
    showCode: Boolean,
): OtpUiState {
    val type = (presentation as? OtpPresentation.Applicable)?.type ?: return OtpUiState.NotApplicable
    val plugin = type.plugin()
    if (!showCode) return OtpUiState.Hidden
    return this?.toUiModel(plugin.formatter) ?: OtpUiState.NotRequested
}

internal fun OtpCodeState?.toOtpUiState(
    type: OtpType?,
    showCode: Boolean = true,
): OtpUiState {
    if (type == null) return OtpUiState.NotApplicable
    if (!showCode) return OtpUiState.Hidden
    return this?.toUiModel(type.plugin().formatter) ?: OtpUiState.NotRequested
}

private fun OtpCodeState.toUiModel(formatter: OtpCodeFormatter): OtpUiState =
    when (this) {
        OtpCodeState.Pending -> OtpUiState.Pending
        is OtpCodeState.TimedReady ->
            OtpUiState.TimedReady(
                currentCode = formatter.format(code),
                nextCode = formatter.format(nextCode),
                progress = progress,
                windowEndMillis = windowEndMillis,
                periodSeconds = periodSeconds,
            )
        is OtpCodeState.HotpReady ->
            OtpUiState.HotpReady(
                currentCode = formatter.format(code),
                counter = counter,
                canRollbackPrevious = canRollbackPrevious,
            )
        is OtpCodeState.Failed -> OtpUiState.Failed(canRollbackPrevious)
}

package com.aozijx.passly.feature.vault.otp.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState

internal data class OtpWindowProgressState(
    val progress: State<Float>,
    val isExpiring: State<Boolean>,
)

@Composable
internal fun rememberOtpWindowProgress(
    state: OtpUiState,
    expiringThreshold: Float = DEFAULT_EXPIRING_PROGRESS_THRESHOLD,
): OtpWindowProgressState {
    val sharedProgressTime = LocalOtpProgressTime.current
    val fallbackProgressState = rememberUpdatedState((state as? OtpUiState.TimedReady)?.progress ?: 0f)
    val progress =
        remember(state, sharedProgressTime, fallbackProgressState) {
            derivedStateOf {
                when (state) {
                    is OtpUiState.TimedReady -> {
                        val periodMillis = state.periodSeconds * 1000L
                        (state.windowEndMillis - sharedProgressTime.value)
                            .coerceIn(0L, periodMillis)
                            .toFloat() / periodMillis
                    }

                    else -> fallbackProgressState.value
                }
            }
        }
    val isExpiring =
        remember(progress, state, expiringThreshold) {
            derivedStateOf {
                state is OtpUiState.TimedReady && progress.value < expiringThreshold
            }
        }
    return OtpWindowProgressState(
        progress = progress,
        isExpiring = isExpiring,
    )
}

private const val DEFAULT_EXPIRING_PROGRESS_THRESHOLD = 0.2f

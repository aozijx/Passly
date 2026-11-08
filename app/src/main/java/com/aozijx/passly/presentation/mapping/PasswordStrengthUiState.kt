package com.aozijx.passly.presentation.mapping

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult

data class PasswordStrengthUiState(
    val progress: Float,
    val color: Color,
    val textResId: Int,
    val isVisible: Boolean,
)

sealed interface PasswordStrengthState {
    data object Hidden : PasswordStrengthState

    data class Visible(val result: PasswordStrengthResult) : PasswordStrengthState
}

internal fun PasswordStrengthState.toUiState(colorScheme: ColorScheme): PasswordStrengthUiState =
    when (this) {
        PasswordStrengthState.Hidden ->
            PasswordStrengthUiState(
                progress = 0f,
                color = colorScheme.onSurfaceVariant,
                textResId = R.string.password_strength_very_weak,
                isVisible = false,
            )
        is PasswordStrengthState.Visible -> result.toUiState(colorScheme, isVisible = true)
    }

internal fun PasswordStrengthResult.toUiState(
    colorScheme: ColorScheme,
    isVisible: Boolean,
): PasswordStrengthUiState {
    val progress = score / 100f
    val (color, textRes) =
        when (level) {
            PasswordStrengthLevel.VERY_WEAK -> colorScheme.error to R.string.password_strength_very_weak
            PasswordStrengthLevel.WEAK -> colorScheme.error.copy(alpha = 0.7f) to R.string.password_strength_weak
            PasswordStrengthLevel.MEDIUM -> colorScheme.tertiary to R.string.password_strength_medium
            PasswordStrengthLevel.GOOD -> colorScheme.secondary to R.string.password_strength_good
            PasswordStrengthLevel.STRONG -> colorScheme.primary to R.string.password_strength_strong
        }
    return PasswordStrengthUiState(progress, color, textRes, isVisible)
}

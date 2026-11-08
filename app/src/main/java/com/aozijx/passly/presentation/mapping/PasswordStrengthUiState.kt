package com.aozijx.passly.presentation.mapping

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult

sealed interface PasswordStrengthUiState {
    data object Hidden : PasswordStrengthUiState

    data class Visible(
        val progress: Float,
        val color: Color,
        @param:StringRes val textResId: Int,
    ) : PasswordStrengthUiState
}

sealed interface PasswordStrengthState {
    data object Hidden : PasswordStrengthState

    data class Visible(val result: PasswordStrengthResult) : PasswordStrengthState
}

internal fun PasswordStrengthState.toUiState(colorScheme: ColorScheme): PasswordStrengthUiState =
    when (this) {
        PasswordStrengthState.Hidden -> PasswordStrengthUiState.Hidden
        is PasswordStrengthState.Visible -> result.toUiState(colorScheme)
    }

internal fun PasswordStrengthResult.toUiState(colorScheme: ColorScheme): PasswordStrengthUiState.Visible =
    toUiState(colorScheme, level.defaultLabelRes())

internal fun PasswordStrengthResult.toUiState(
    colorScheme: ColorScheme,
    @StringRes textResId: Int,
): PasswordStrengthUiState.Visible {
    val progress = score / 100f
    val color =
        when (level) {
            PasswordStrengthLevel.VERY_WEAK -> colorScheme.error
            PasswordStrengthLevel.WEAK -> colorScheme.error.copy(alpha = 0.7f)
            PasswordStrengthLevel.MEDIUM -> colorScheme.tertiary
            PasswordStrengthLevel.GOOD -> colorScheme.secondary
            PasswordStrengthLevel.STRONG -> colorScheme.primary
        }
    return PasswordStrengthUiState.Visible(progress, color, textResId)
}

private fun PasswordStrengthLevel.defaultLabelRes(): Int =
    when (this) {
        PasswordStrengthLevel.VERY_WEAK -> R.string.password_strength_very_weak
        PasswordStrengthLevel.WEAK -> R.string.password_strength_weak
        PasswordStrengthLevel.MEDIUM -> R.string.password_strength_medium
        PasswordStrengthLevel.GOOD -> R.string.password_strength_good
        PasswordStrengthLevel.STRONG -> R.string.password_strength_strong
    }

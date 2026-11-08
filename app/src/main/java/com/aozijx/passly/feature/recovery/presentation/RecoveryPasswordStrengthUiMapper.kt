package com.aozijx.passly.feature.recovery.presentation

import androidx.compose.material3.ColorScheme
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import com.aozijx.passly.presentation.mapping.PasswordStrengthUiState
import com.aozijx.passly.presentation.mapping.toUiState

internal fun PasswordStrengthResult?.toRecoveryPasswordStrengthUiState(colorScheme: ColorScheme): PasswordStrengthUiState =
    this?.let { it.toUiState(colorScheme, it.level.recoveryLabelRes()) } ?: PasswordStrengthUiState.Hidden

private fun PasswordStrengthLevel.recoveryLabelRes(): Int =
    when (this) {
        PasswordStrengthLevel.VERY_WEAK -> R.string.recovery_mode_password_strength_very_weak
        PasswordStrengthLevel.WEAK -> R.string.recovery_mode_password_strength_weak
        PasswordStrengthLevel.MEDIUM -> R.string.recovery_mode_password_strength_medium
        PasswordStrengthLevel.GOOD -> R.string.recovery_mode_password_strength_good
        PasswordStrengthLevel.STRONG -> R.string.recovery_mode_password_strength_strong
    }

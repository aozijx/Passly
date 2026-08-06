package com.aozijx.passly.core.ui.components.settings

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.aozijx.passly.R
import com.aozijx.passly.core.security.password.PasswordStrengthEngine
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult

data class PasswordStrengthUiState(
    val progress: Float,
    val color: Color,
    val textResId: Int,
    val level: PasswordStrengthLevel
)

fun PasswordStrengthResult.toUiState(
    colorScheme: ColorScheme
): PasswordStrengthUiState {
    val progress = this.score / 100f
    val (color, textRes) = when (this.level) {
        PasswordStrengthLevel.VERY_WEAK -> colorScheme.error to R.string.password_strength_very_weak
        PasswordStrengthLevel.WEAK -> colorScheme.error.copy(alpha = 0.7f) to R.string.password_strength_weak
        PasswordStrengthLevel.MEDIUM -> colorScheme.tertiary to R.string.password_strength_medium
        PasswordStrengthLevel.GOOD -> colorScheme.secondary to R.string.password_strength_good
        PasswordStrengthLevel.STRONG -> colorScheme.primary to R.string.password_strength_strong
    }
    return PasswordStrengthUiState(progress, color, textRes, this.level)
}

@Composable
fun rememberPasswordStrength(password: String): PasswordStrengthUiState {
    val colorScheme = MaterialTheme.colorScheme
    return remember(password, colorScheme) {
        PasswordStrengthEngine.evaluate(password).toUiState(colorScheme)
    }
}

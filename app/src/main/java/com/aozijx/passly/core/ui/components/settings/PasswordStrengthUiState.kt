package com.aozijx.passly.core.ui.components.settings

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.aozijx.passly.R
import com.aozijx.passly.core.util.PasswordStrengthEngine
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult

/**
 * UI 状态数据类，包装显示所需的所有信息
 */
data class PasswordStrengthUiState(
    val progress: Float,        // 0..1，用于进度条
    val color: Color,
    val textResId: Int,         // 描述性文字
    val level: PasswordStrengthLevel
)

/**
 * 将算法结果转换为 UI 状态
 */
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

/**
 * 在 Composable 中记忆并获取密码强度状态
 */
@Composable
fun rememberPasswordStrength(password: String): PasswordStrengthUiState {
    val colorScheme = MaterialTheme.colorScheme
    return remember(password, colorScheme) {
        PasswordStrengthEngine.evaluate(password).toUiState(colorScheme)
    }
}

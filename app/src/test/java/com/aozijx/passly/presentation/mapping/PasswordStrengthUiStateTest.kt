package com.aozijx.passly.presentation.mapping

import androidx.compose.material3.lightColorScheme
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthUiStateTest {
    @Test
    fun mapsEveryDomainStrengthLevelToItsAppLabelAndThemeColor() {
        val colorScheme = lightColorScheme()
        val mappings =
            listOf(
                Triple(PasswordStrengthLevel.VERY_WEAK, colorScheme.error, R.string.password_strength_very_weak),
                Triple(PasswordStrengthLevel.WEAK, colorScheme.error.copy(alpha = 0.7f), R.string.password_strength_weak),
                Triple(PasswordStrengthLevel.MEDIUM, colorScheme.tertiary, R.string.password_strength_medium),
                Triple(PasswordStrengthLevel.GOOD, colorScheme.secondary, R.string.password_strength_good),
                Triple(PasswordStrengthLevel.STRONG, colorScheme.primary, R.string.password_strength_strong),
            )

        mappings.forEachIndexed { index, (level, color, textResId) ->
            val score = (index + 1) * 10
            val uiState = PasswordStrengthResult(score, level).toUiState(colorScheme)

            assertEquals(score / 100f, uiState.progress)
            assertEquals(color, uiState.color)
            assertEquals(textResId, uiState.textResId)
        }
    }
}

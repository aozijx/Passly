package com.aozijx.passly.presentation.mapping

import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

sealed interface PasswordConfirmationUiState {
    data object Hidden : PasswordConfirmationUiState

    data object Mismatch : PasswordConfirmationUiState
}

data class PasswordSetupDraft(
    val newPassword: SensitiveValue = EmptySensitiveValue,
    val confirmPassword: SensitiveValue = EmptySensitiveValue,
    val passwordStrength: PasswordStrengthResult? = null,
) {
    val confirmationState: PasswordConfirmationUiState
        get() =
            withPasswordChars { passwordChars, confirmationChars ->
                if (confirmationChars.isNotEmpty() && !passwordChars.contentEquals(confirmationChars)) {
                    PasswordConfirmationUiState.Mismatch
                } else {
                    PasswordConfirmationUiState.Hidden
                }
            }

    fun canSubmit(): Boolean =
        withPasswordChars { passwordChars, confirmationChars ->
            AppPasswordPolicy.DEFAULT.acceptsLength(passwordChars.size) &&
                passwordChars.contentEquals(confirmationChars)
        }

    private fun <T> withPasswordChars(block: (CharArray, CharArray) -> T): T {
        val passwordChars = newPassword.toCharArray()
        return try {
            val confirmationChars = confirmPassword.toCharArray()
            try {
                block(passwordChars, confirmationChars)
            } finally {
                MemoryCleaner.wipeCharArray(confirmationChars)
            }
        } finally {
            MemoryCleaner.wipeCharArray(passwordChars)
        }
    }
}

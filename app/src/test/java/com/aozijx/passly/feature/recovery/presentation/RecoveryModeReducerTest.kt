package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.feature.recovery.contract.RecoveryModeUiState
import com.aozijx.passly.security.crypto.SecureString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryModeReducerTest {

    @Test
    fun `successful password setup clears sensitive references`() {
        val password = SecureString.fromString("temporary")
        val confirm = SecureString.fromString("temporary")
        try {
            val result = RecoveryModeReducer.reduce(
                RecoveryModeUiState(
                    showSetPasswordDialog = true,
                    newPassword = password,
                    confirmPassword = confirm,
                    isSettingPassword = true,
                ),
                RecoveryModeMutation.PasswordSetupCompleted,
            )

            assertFalse(result.showSetPasswordDialog)
            assertFalse(result.isSettingPassword)
            assertSame(EmptySensitiveValue, result.newPassword)
            assertSame(EmptySensitiveValue, result.confirmPassword)
        } finally {
            password.wipe()
            confirm.wipe()
        }
    }

    @Test
    fun `rejected recovery mode closes setup and exposes error`() {
        val result = RecoveryModeReducer.reduce(
            RecoveryModeUiState(
                showSetPasswordDialog = true,
                isSettingPassword = true,
            ),
            RecoveryModeMutation.RecoveryModeRejected("当前不在恢复模式"),
        )

        assertFalse(result.showSetPasswordDialog)
        assertFalse(result.isSettingPassword)
        assertEquals("当前不在恢复模式", result.passwordSetupError)
    }

    @Test
    fun `opening dialog preserves no full access state`() {
        val result = RecoveryModeReducer.reduce(
            RecoveryModeUiState(),
            RecoveryModeMutation.PasswordDialogOpened,
        )

        assertTrue(result.showSetPasswordDialog)
    }
}

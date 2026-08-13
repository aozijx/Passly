package com.aozijx.passly.app.shell.presentation

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.app.shell.contract.AppShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellReducerTest {

    @Test
    fun `recovery mode clears database failure and full authorization`() {
        val result = AppShellReducer.reduce(
            AppShellUiState(
                isAuthorized = true,
                isDatabaseInitializing = true,
                databaseError = IllegalStateException("failure"),
            ),
            AppShellMutation.RecoveryModeEntered,
        )

        assertFalse(result.isAuthorized)
        assertTrue(result.isRecoveryMode)
        assertFalse(result.isDatabaseInitializing)
        assertNull(result.databaseError)
    }

    @Test
    fun `database retry clears stale error but recovery authentication keeps it`() {
        val error = IllegalStateException("failure")
        val initial = AppShellUiState(databaseError = error)

        val retry = AppShellReducer.reduce(
            initial,
            AppShellMutation.DatabaseInitializationStarted(clearError = true),
        )
        val recovery = AppShellReducer.reduce(
            initial,
            AppShellMutation.DatabaseInitializationStarted(clearError = false),
        )

        assertTrue(retry.isDatabaseInitializing)
        assertNull(retry.databaseError)
        assertSame(error, recovery.databaseError)
    }

    @Test
    fun `settings projection changes only shell appearance fields`() {
        val error = IllegalStateException("keep")
        val result = AppShellReducer.reduce(
            AppShellUiState(isAuthorized = true, databaseError = error),
            AppShellMutation.SettingsChanged(
                appearance = AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    isDynamicColor = false,
                    language = AppLanguage.EN,
                    fontFamily = FontFamilyMode.SYSTEM,
                ),
                interfaceSettings = InterfaceSettings(
                    outerCornerRadiusDp = 30f,
                    innerCornerRadiusDp = 12f,
                    groupItemSpacingDp = 4f,
                    groupContentPaddingDp = 10f,
                ),
            ),
        )

        assertEquals(ThemeMode.DARK, result.themeMode)
        assertFalse(result.isDynamicColor)
        assertEquals(AppLanguage.EN, result.language)
        assertEquals(30f, result.outerCornerRadiusDp)
        assertTrue(result.isAuthorized)
        assertSame(error, result.databaseError)
    }
}

package com.aozijx.passly.feature.main.presentation

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.feature.main.contract.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainReducerTest {

    @Test
    fun `recovery mode clears database failure and full authorization`() {
        val result = MainReducer.reduce(
            MainUiState(
                isAuthorized = true,
                isDatabaseInitializing = true,
                databaseError = IllegalStateException("failure"),
            ),
            MainMutation.RecoveryModeEntered,
        )

        assertFalse(result.isAuthorized)
        assertTrue(result.isRecoveryMode)
        assertFalse(result.isDatabaseInitializing)
        assertNull(result.databaseError)
    }

    @Test
    fun `database retry clears stale error but recovery authentication keeps it`() {
        val error = IllegalStateException("failure")
        val initial = MainUiState(databaseError = error)

        val retry = MainReducer.reduce(
            initial,
            MainMutation.DatabaseInitializationStarted(clearError = true),
        )
        val recovery = MainReducer.reduce(
            initial,
            MainMutation.DatabaseInitializationStarted(clearError = false),
        )

        assertTrue(retry.isDatabaseInitializing)
        assertNull(retry.databaseError)
        assertSame(error, recovery.databaseError)
    }

    @Test
    fun `settings projection changes only shell appearance fields`() {
        val error = IllegalStateException("keep")
        val result = MainReducer.reduce(
            MainUiState(isAuthorized = true, databaseError = error),
            MainMutation.SettingsChanged(
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

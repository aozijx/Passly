package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.presentation.feature.shell.AppShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellReducerTest {

    @Test
    fun `session projection changes without overwriting database failure state`() {
        val error = IllegalStateException("failure")
        val result = AppShellReducer.reduce(
            AppShellUiState(
                sessionMode = AppShellSessionMode.VAULT,
                isDatabaseRetrying = true,
                databaseError = error,
            ),
            AppShellMutation.SessionChanged(AppShellSessionMode.RECOVERY),
        )

        assertEquals(AppShellSessionMode.RECOVERY, result.sessionMode)
        assertTrue(result.isDatabaseRetrying)
        assertSame(error, result.databaseError)
    }

    @Test
    fun `database retry keeps authoritative error visible and marks retry in progress`() {
        val error = IllegalStateException("failure")
        val initial = AppShellUiState(databaseError = error)

        val retry = AppShellReducer.reduce(
            initial,
            AppShellMutation.DatabaseRetryStarted,
        )

        assertTrue(retry.isDatabaseRetrying)
        assertSame(error, retry.databaseError)
    }

    @Test
    fun `database failure stream clears error and retry state together`() {
        val result = AppShellReducer.reduce(
            AppShellUiState(
                isDatabaseRetrying = true,
                databaseError = IllegalStateException("failure"),
            ),
            AppShellMutation.DatabaseFailureChanged(error = null),
        )

        assertFalse(result.isDatabaseRetrying)
        assertEquals(null, result.databaseError)
    }

    @Test
    fun `database retry completion only ends busy state`() {
        val error = IllegalStateException("failure")
        val result = AppShellReducer.reduce(
            AppShellUiState(
                isDatabaseRetrying = true,
                databaseError = error,
            ),
            AppShellMutation.DatabaseRetryFinished,
        )

        assertFalse(result.isDatabaseRetrying)
        assertSame(error, result.databaseError)
    }

    @Test
    fun `settings projection changes shell appearance and window policy`() {
        val error = IllegalStateException("keep")
        val appearance = AppearanceSettings(
            themeMode = ThemeMode.DARK,
            isDynamicColor = false,
            language = AppLanguage.EN,
            fontFamily = FontFamilyMode.SYSTEM,
        )
        val result = AppShellReducer.reduce(
            AppShellUiState(
                sessionMode = AppShellSessionMode.VAULT,
                databaseError = error,
            ),
            AppShellMutation.SettingsChanged(
                appearance = appearance,
                interfaceSettings = InterfaceSettings(appCornerRadiusDp = 30f),
                securitySettings = SecuritySettings(
                    isSecureContentEnabled = false,
                ),
            ),
        )

        assertSame(appearance, result.appearance)
        assertEquals(30f, result.appCornerRadiusDp)
        assertFalse(result.windowPolicy.isSecureContentEnabled)
        assertEquals(AppShellSessionMode.VAULT, result.sessionMode)
        assertSame(error, result.databaseError)
    }
}

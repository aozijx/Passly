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
import org.junit.Test

class AppShellReducerTest {

    @Test
    fun `startup window policy protects content without fabricating persisted system bar settings`() {
        val state = AppShellUiState()
        val policy = state.windowPolicy

        assertSame(AppShellSettingsState.Loading, state.settings)
        assertEquals(true, policy.isSecureContentEnabled)
        assertEquals(false, policy.isStatusBarAutoHide)
    }

    @Test
    fun `session projection changes without overwriting database failure state`() {
        val error = IllegalStateException("failure")
        val result = AppShellReducer.reduce(
            AppShellUiState(
                sessionMode = AppShellSessionMode.VAULT,
                databaseState = AppShellDatabaseState.Failed(
                    cause = error,
                    retryPhase = DatabaseRetryPhase.Retrying,
                ),
            ),
            AppShellMutation.SessionChanged(AppShellSessionMode.RECOVERY),
        )

        assertEquals(AppShellSessionMode.RECOVERY, result.sessionMode)
        val databaseState = result.databaseState as AppShellDatabaseState.Failed
        assertEquals(DatabaseRetryPhase.Retrying, databaseState.retryPhase)
        assertSame(error, databaseState.cause)
    }

    @Test
    fun `database retry keeps authoritative error visible and marks retry in progress`() {
        val error = IllegalStateException("failure")
        val initial = AppShellUiState(
            databaseState = AppShellDatabaseState.Failed(error),
        )

        val retry = AppShellReducer.reduce(
            initial,
            AppShellMutation.DatabaseRetryStarted,
        )

        val databaseState = retry.databaseState as AppShellDatabaseState.Failed
        assertEquals(DatabaseRetryPhase.Retrying, databaseState.retryPhase)
        assertSame(error, databaseState.cause)
    }

    @Test
    fun `database failure stream clears error and retry state together`() {
        val result = AppShellReducer.reduce(
            AppShellUiState(
                databaseState = AppShellDatabaseState.Failed(
                    cause = IllegalStateException("failure"),
                    retryPhase = DatabaseRetryPhase.Retrying,
                ),
            ),
            AppShellMutation.DatabaseAvailable,
        )

        assertSame(AppShellDatabaseState.Available, result.databaseState)
    }

    @Test
    fun `database retry completion only ends busy state`() {
        val error = IllegalStateException("failure")
        val result = AppShellReducer.reduce(
            AppShellUiState(
                databaseState = AppShellDatabaseState.Failed(
                    cause = error,
                    retryPhase = DatabaseRetryPhase.Retrying,
                ),
            ),
            AppShellMutation.DatabaseRetryFinished,
        )

        val databaseState = result.databaseState as AppShellDatabaseState.Failed
        assertEquals(DatabaseRetryPhase.Idle, databaseState.retryPhase)
        assertSame(error, databaseState.cause)
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
                databaseState = AppShellDatabaseState.Failed(error),
            ),
            AppShellMutation.SettingsChanged(
                appearance = appearance,
                interfaceSettings = InterfaceSettings(appCornerRadiusDp = 30f),
                securitySettings = SecuritySettings(
                    isSecureContentEnabled = false,
                ),
            ),
        )

        val settings = result.settings as AppShellSettingsState.Ready
        assertSame(appearance, settings.appearance)
        assertEquals(30f, settings.appCornerRadiusDp)
        assertFalse(settings.windowPolicy.isSecureContentEnabled)
        assertEquals(AppShellSessionMode.VAULT, result.sessionMode)
        assertSame(error, (result.databaseState as AppShellDatabaseState.Failed).cause)
    }
}

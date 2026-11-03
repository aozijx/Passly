package com.aozijx.passly.feature.shell.presentation

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings

internal sealed interface AppShellMutation {
    data class SessionChanged(
        val mode: AppShellSessionMode,
    ) : AppShellMutation

    data class SettingsChanged(
        val appearance: AppearanceSettings,
        val interfaceSettings: InterfaceSettings,
        val securitySettings: SecuritySettings,
    ) : AppShellMutation

    data object SettingsLoading : AppShellMutation

    data class SettingsFailed(
        val failure: ReadFailure,
    ) : AppShellMutation

    data object DatabaseRetryStarted : AppShellMutation

    data object DatabaseRetryFinished : AppShellMutation

    data object DatabaseAvailable : AppShellMutation

    data class DatabaseFailed(
        val cause: Throwable,
        val retryPhase: DatabaseRetryPhase = DatabaseRetryPhase.Idle,
    ) : AppShellMutation
}

internal object AppShellReducer {
    fun reduce(
        state: AppShellUiState,
        mutation: AppShellMutation,
    ): AppShellUiState =
        when (mutation) {
            is AppShellMutation.SessionChanged -> {
                state.copy(sessionMode = mutation.mode)
            }

            is AppShellMutation.SettingsChanged -> {
                state.copy(
                    settings =
                        AppShellSettingsState.Ready(
                            appearance = mutation.appearance,
                            appCornerRadiusDp = mutation.interfaceSettings.appCornerRadiusDp,
                            windowPolicy =
                                AppWindowPolicy(
                                    isSecureContentEnabled = mutation.securitySettings.isSecureContentEnabled,
                                    isStatusBarAutoHide = mutation.interfaceSettings.hideSystemBars,
                                ),
                        ),
                )
            }

            AppShellMutation.SettingsLoading -> {
                state.copy(settings = AppShellSettingsState.Loading)
            }

            is AppShellMutation.SettingsFailed -> {
                state.copy(settings = AppShellSettingsState.Failed(mutation.failure))
            }

            AppShellMutation.DatabaseRetryStarted -> {
                state.updateDatabaseFailure {
                    copy(retryPhase = DatabaseRetryPhase.Retrying)
                }
            }

            AppShellMutation.DatabaseRetryFinished -> {
                state.updateDatabaseFailure {
                    copy(retryPhase = DatabaseRetryPhase.Idle)
                }
            }

            AppShellMutation.DatabaseAvailable -> {
                state.copy(
                    databaseState = AppShellDatabaseState.Available,
                )
            }

            is AppShellMutation.DatabaseFailed -> {
                state.copy(
                    databaseState = AppShellDatabaseState.Failed(mutation.cause, mutation.retryPhase),
                )
            }
        }
}

private inline fun AppShellUiState.updateDatabaseFailure(
    transform: AppShellDatabaseState.Failed.() -> AppShellDatabaseState.Failed,
): AppShellUiState {
    val failure = databaseState as? AppShellDatabaseState.Failed ?: return this
    return copy(databaseState = failure.transform())
}

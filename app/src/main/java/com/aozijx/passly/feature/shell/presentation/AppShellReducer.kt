package com.aozijx.passly.feature.shell.presentation

import com.aozijx.passly.app.settings.RecoverableReadState

internal sealed interface AppShellMutation {
    data class SessionChanged(
        val mode: AppShellSessionMode,
    ) : AppShellMutation

    data class SettingsStateChanged(
        val state: RecoverableReadState<AppShellSettings>,
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

            is AppShellMutation.SettingsStateChanged -> {
                state.copy(settings = mutation.state)
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

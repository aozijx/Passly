package com.aozijx.passly.presentation.feature.shell

import android.view.Window
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.feature.recovery.RecoveryModeRoute
import com.aozijx.passly.presentation.feature.shell.ui.DatabaseErrorDialog
import com.aozijx.passly.presentation.feature.unlock.AuthenticationRoute
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AppShell(
    window: Window,
    uiState: AppShellUiState,
    effects: Flow<AppShellEffect>,
    onAction: (AppShellUiAction) -> Unit,
    onCloseApp: () -> Unit,
) {
    val context = LocalContext.current
    val noticePublisher = LocalAppNoticePublisher.current

    LaunchedEffect(effects, context) {
        effects.collect { effect ->
            when (effect) {
                is AppShellEffect.ShowError -> Toast.makeText(
                    context,
                    effect.error,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    SecureShellDestination(uiState.destination) { destination ->
        when (destination) {
            AppShellDestination.DATABASE_ERROR -> {
                val databaseState = uiState.databaseState as AppShellDatabaseState.Failed
                DatabaseErrorDialog(
                    isBusy = databaseState.retryPhase == DatabaseRetryPhase.Retrying,
                    onRetry = { onAction(AppShellUiAction.RetryDatabaseSession) },
                    onCloseApp = {
                        noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER))
                        window.decorView.postDelayed(onCloseApp, APP_CLOSE_MESSAGE_DELAY_MS)
                    },
                )
            }

            AppShellDestination.VAULT -> PasslyAppNavigation()
            AppShellDestination.RECOVERY -> RecoveryModeRoute(
                onExit = { onAction(AppShellUiAction.ExitRecovery) },
            )

            AppShellDestination.AUTHENTICATION -> AuthenticationRoute()
        }
    }

    AppWindowPolicyEffects(
        window = window,
        policy = uiState.windowPolicy,
    )
}

/**
 * Switches authentication domains without retaining outgoing sensitive composition.
 *
 * Vault content owns a NavHost and graph-scoped ViewModels. Keeping it alive for a visual
 * crossfade after the secure session changes can recompose it after that NavHost is cleared.
 */
@Composable
internal fun SecureShellDestination(
    destination: AppShellDestination,
    content: @Composable (AppShellDestination) -> Unit,
) {
    key(destination) {
        content(destination)
    }
}

private const val APP_CLOSE_MESSAGE_DELAY_MS = 1_000L

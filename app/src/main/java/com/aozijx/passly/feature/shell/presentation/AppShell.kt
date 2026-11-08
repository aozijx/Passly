package com.aozijx.passly.feature.shell.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import com.aozijx.passly.app.lifecycle.APP_CLOSE_REMINDER_DELAY_MS
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.feature.shell.presentation.ui.DatabaseErrorDialog
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AppShell(
    uiState: AppShellUiState,
    effects: Flow<AppShellEffect>,
    onAction: (AppShellUiAction) -> Unit,
    onCloseApp: () -> Unit,
    vaultContent: @Composable () -> Unit,
    recoveryContent: @Composable (onExit: () -> Unit) -> Unit,
    authenticationContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val view = LocalView.current
    val noticePublisher = LocalAppNoticePublisher.current

    LaunchedEffect(effects, context, resources) {
        effects.collect { effect ->
            when (effect) {
                is AppShellEffect.ShowError -> {
                    Toast
                        .makeText(
                            context,
                            resources.getString(effect.error.resourceId),
                            Toast.LENGTH_LONG,
                        ).show()
                }
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
                        view.postDelayed(onCloseApp, APP_CLOSE_REMINDER_DELAY_MS)
                    },
                )
            }

            AppShellDestination.VAULT -> {
                vaultContent()
            }

            AppShellDestination.RECOVERY -> {
                recoveryContent {
                    onAction(AppShellUiAction.ExitRecovery)
                }
            }

            AppShellDestination.AUTHENTICATION -> {
                authenticationContent()
            }
        }
    }
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

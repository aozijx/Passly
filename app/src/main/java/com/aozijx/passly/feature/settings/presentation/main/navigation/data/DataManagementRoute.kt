package com.aozijx.passly.feature.settings.presentation.main.navigation.data

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.database.presentation.reset.DatabaseResetRoute
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSettingsAction
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSettingsEffect
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.data.CacheSettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.data.DataManagementSection
import com.aozijx.passly.feature.settings.presentation.ui.data.LogSettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataManagementRoute(
    onOpenTrash: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: StorageSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatabaseReset by rememberSaveable { mutableStateOf(false) }
    var showLogClearConfirmation by rememberSaveable { mutableStateOf(false) }
    val cacheClearedMessage = stringResource(R.string.settings_storage_cache_cleared)
    val logsClearedMessage = stringResource(R.string.settings_log_cleared)
    val logClearFailedMessage = stringResource(R.string.settings_log_clear_failed)

    LaunchedEffect(viewModel) {
        viewModel.onAction(StorageSettingsAction.PageEntered)
    }
    LaunchedEffect(viewModel, cacheClearedMessage, logsClearedMessage, logClearFailedMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StorageSettingsEffect.CacheCleared ->
                    Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
                StorageSettingsEffect.LogsCleared ->
                    Toast.makeText(context, logsClearedMessage, Toast.LENGTH_SHORT).show()
                StorageSettingsEffect.LogClearFailed ->
                    Toast.makeText(context, logClearFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
        onBack = onBack,
    ) {
        item {
            DataManagementSection(
                onOpenTrash = onOpenTrash,
                onOpenDatabaseReset = {
                    showDatabaseReset = true
                },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            SettingsSection {
                CacheSettingsSection(
                    state = state.cache,
                    onClearCache = { viewModel.onAction(StorageSettingsAction.ClearCache) },
                )
                Spacer(Modifier.height(24.dp))
                LogSettingsSection(
                    state = state.logs,
                    onOpenDiagnostics = onOpenDiagnostics,
                    onClearLogs = { showLogClearConfirmation = true },
                )
            }
        }
    }

    if (showDatabaseReset) {
        DatabaseResetRoute(onDismiss = { showDatabaseReset = false })
    }
    if (showLogClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogClearConfirmation = false },
            title = { Text(stringResource(R.string.settings_log_clear_action)) },
            text = { Text(stringResource(R.string.settings_log_clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogClearConfirmation = false
                        viewModel.onAction(StorageSettingsAction.ClearLogs)
                    },
                ) {
                    Text(stringResource(R.string.settings_log_clear_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

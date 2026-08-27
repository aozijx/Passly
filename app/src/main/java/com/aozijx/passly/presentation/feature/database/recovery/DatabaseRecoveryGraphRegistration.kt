package com.aozijx.passly.presentation.feature.database.recovery

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.shell.navigation.ShellNavigationContext
import com.aozijx.passly.presentation.ui.database.recovery.DatabaseRecoverySheet

internal fun NavGraphBuilder.registerDatabaseRecoveryGraph(
    context: ShellNavigationContext,
) {
    composable(AppRoute.DatabaseRecovery.route) {
        val viewModel: DatabaseRecoveryViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        DatabaseRecoverySheet(
            visible = true,
            state = state.toSheetState(),
            onDismiss = context.navigateBack,
            onClearResult = {
                viewModel.onAction(DatabaseRecoveryUiAction.ClearRecoveryResult)
            },
            onScan = {
                viewModel.onAction(DatabaseRecoveryUiAction.ScanRecoveryPackage(it))
            },
            onRestore = {
                viewModel.onAction(DatabaseRecoveryUiAction.RestoreRecoveryPackage(it))
            },
            onToggleType = {
                viewModel.onAction(
                    DatabaseRecoveryUiAction.ToggleRecoveryType(EntryType.valueOf(it)),
                )
            },
            onDelete = {
                viewModel.onAction(DatabaseRecoveryUiAction.DeleteRecoveryPackage(it))
            },
            onClearDatabase = {
                viewModel.onAction(DatabaseRecoveryUiAction.ClearDatabase)
            },
        )
    }
}

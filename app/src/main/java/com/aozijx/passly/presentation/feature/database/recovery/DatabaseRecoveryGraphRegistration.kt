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
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoveryEventHandler

internal fun NavGraphBuilder.registerDatabaseRecoveryGraph(
    context: ShellNavigationContext,
) {
    composable(AppRoute.DatabaseRecovery.route) {
        val viewModel: DatabaseRecoveryViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        DatabaseRecoverySheet(
            state = state.toSheetState(),
            eventHandler = object : DatabaseRecoveryEventHandler {
                override fun onDismiss() = context.navigateBack()
                override fun onClearResult() =
                    viewModel.onAction(DatabaseRecoveryUiAction.ClearRecoveryResult)
                override fun onScan(packageId: String) =
                    viewModel.onAction(DatabaseRecoveryUiAction.ScanRecoveryPackage(packageId))
                override fun onRestore(packageId: String) =
                    viewModel.onAction(DatabaseRecoveryUiAction.RestoreRecoveryPackage(packageId))
                override fun onToggleType(typeId: String) = viewModel.onAction(
                    DatabaseRecoveryUiAction.ToggleRecoveryType(EntryType.valueOf(typeId)),
                )
                override fun onDelete(packageId: String) =
                    viewModel.onAction(DatabaseRecoveryUiAction.DeleteRecoveryPackage(packageId))
                override fun onClearDatabase() =
                    viewModel.onAction(DatabaseRecoveryUiAction.ClearDatabase)
            },
        )
    }
}

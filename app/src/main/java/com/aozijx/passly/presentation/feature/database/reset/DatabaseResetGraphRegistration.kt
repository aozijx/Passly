package com.aozijx.passly.presentation.feature.database.reset

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.shell.navigation.ShellNavigationContext
import com.aozijx.passly.presentation.ui.database.reset.DatabaseResetSheet
import com.aozijx.passly.presentation.ui.database.reset.model.DatabaseResetEventHandler
import com.aozijx.passly.presentation.ui.database.reset.model.DatabaseResetSheetState

internal fun NavGraphBuilder.registerDatabaseResetGraph(
    context: ShellNavigationContext,
) {
    composable(AppRoute.DatabaseReset.route) {
        val viewModel: DatabaseResetViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        DatabaseResetSheet(
            state = DatabaseResetSheetState(
                isResetting = state.isResetting,
                isResetComplete = state.isResetComplete,
                error = state.error,
            ),
            eventHandler = object : DatabaseResetEventHandler {
                override fun onDismiss() = context.navigateBack()
                override fun onReset() =
                    viewModel.onAction(DatabaseResetUiAction.Reset)
            },
        )
    }
}
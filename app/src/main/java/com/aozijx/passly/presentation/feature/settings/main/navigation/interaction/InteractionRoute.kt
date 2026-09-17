package com.aozijx.passly.presentation.feature.settings.main.navigation.interaction

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsAction
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.toUiModel
import com.aozijx.passly.presentation.ui.settings.interaction.InteractionDetail
import com.aozijx.passly.presentation.ui.settings.interaction.SwipeActionSelectDialog
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup

private enum class SwipeActionDialog { Left, Right }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InteractionRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: InteractionSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiState = state.toUiModel()
    val context = LocalContext.current
    var activeDialog by rememberSaveable { mutableStateOf<SwipeActionDialog?>(null) }

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                InteractionSettingsEffect.Saved -> context.getString(R.string.settings_saved)
                is InteractionSettingsEffect.SaveFailed -> effect.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERACTION.titleRes),
        onBack = onBack,
    ) {
        item {
            InteractionDetail(
                state = uiState,
                onSwipeEnabledChange = {
                    viewModel.onAction(InteractionSettingsAction.SetSwipeEnabled(it))
                },
                onLeftSwipeActionClick = { activeDialog = SwipeActionDialog.Left },
                onRightSwipeActionClick = { activeDialog = SwipeActionDialog.Right },
            )
        }
    }

    when (activeDialog) {
        SwipeActionDialog.Left -> SwipeActionSelectDialog(
            title = stringResource(R.string.settings_swipe_select_left_action),
            currentAction = uiState.swipeLeftAction,
            onActionSelected = {
                viewModel.onAction(InteractionSettingsAction.SetSwipeLeftAction(it))
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
        SwipeActionDialog.Right -> SwipeActionSelectDialog(
            title = stringResource(R.string.settings_swipe_select_right_action),
            currentAction = uiState.swipeRightAction,
            onActionSelected = {
                viewModel.onAction(InteractionSettingsAction.SetSwipeRightAction(it))
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
        null -> Unit
    }
}

package com.aozijx.passly.feature.settings.presentation.main.navigation.interaction

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.presentation.main.interaction.InteractionSettingsAction
import com.aozijx.passly.feature.settings.presentation.main.interaction.InteractionSettingsEffect
import com.aozijx.passly.feature.settings.presentation.main.interaction.InteractionSettingsUiState
import com.aozijx.passly.feature.settings.presentation.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.interaction.InteractionDetail
import com.aozijx.passly.feature.settings.presentation.ui.interaction.SwipeActionSelectDialog
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

private enum class SwipeActionDialog { Left, Right }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InteractionRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: InteractionSettingsViewModel = hiltViewModel()
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val savedMessage = stringResource(R.string.settings_saved)
    var activeDialog by rememberSaveable { mutableStateOf<SwipeActionDialog?>(null) }

    LaunchedEffect(viewModel, context, resources, savedMessage) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                InteractionSettingsEffect.Saved -> savedMessage
                is InteractionSettingsEffect.SaveFailed -> resources.getString(effect.message.resourceId)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERACTION.titleRes),
        onBack = onBack,
    ) {
        when (val readyState = state) {
            InteractionSettingsUiState.Loading -> Unit
            is InteractionSettingsUiState.Ready -> item {
                InteractionDetail(
                    state = readyState,
                    onSwipeEnabledChange = {
                        viewModel.onAction(InteractionSettingsAction.SetSwipeEnabled(it))
                    },
                    onPullToSearchEnabledChange = {
                        viewModel.onAction(InteractionSettingsAction.SetPullToSearchEnabled(it))
                    },
                    onLeftSwipeActionClick = { activeDialog = SwipeActionDialog.Left },
                    onRightSwipeActionClick = { activeDialog = SwipeActionDialog.Right },
                )
            }
        }
    }

    val readyState = state
    if (readyState is InteractionSettingsUiState.Ready) {
        when (activeDialog) {
            SwipeActionDialog.Left -> SwipeActionSelectDialog(
                title = stringResource(R.string.settings_swipe_select_left_action),
                currentAction = readyState.swipeLeftAction,
                onActionSelected = {
                    viewModel.onAction(InteractionSettingsAction.SetSwipeLeftAction(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null },
            )
            SwipeActionDialog.Right -> SwipeActionSelectDialog(
                title = stringResource(R.string.settings_swipe_select_right_action),
                currentAction = readyState.swipeRightAction,
                onActionSelected = {
                    viewModel.onAction(InteractionSettingsAction.SetSwipeRightAction(it))
                    activeDialog = null
                },
                onDismiss = { activeDialog = null },
            )
            null -> Unit
        }
    }
}

package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsUiState
import com.aozijx.passly.feature.settings.presentation.appearance.toInterfaceUiModel
import com.aozijx.passly.feature.settings.presentation.ui.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

@Composable
internal fun InterfaceRoute(
    onBack: (() -> Unit)?
) {
    val viewModel: InterfaceSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, context, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.errors.collect { error ->
                Toast.makeText(context, resources.getString(error.resourceId), Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERFACE.titleRes),
        onBack = onBack
    ) {
        when (val readyState = state) {
            InterfaceSettingsUiState.Loading -> Unit
            is InterfaceSettingsUiState.Ready -> item {
                InterfaceDetail(
                    state = readyState.toInterfaceUiModel(),
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}

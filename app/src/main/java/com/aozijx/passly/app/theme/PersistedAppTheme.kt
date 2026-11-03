package com.aozijx.passly.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent

/** Renders only after the persisted appearance and interface preferences are available. */
@Composable
internal fun PersistedAppTheme(content: @Composable () -> Unit) {
    val viewModel: AppThemeSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentState = state
    when (currentState) {
        AppThemeSettingsState.Loading -> {
            return
        }

        is AppThemeSettingsState.Failed -> {
            SettingsReadFailureContent(onRetry = viewModel::retry)
        }

        is AppThemeSettingsState.Ready -> {
            AppTheme(
                appearance = currentState.appearance,
                appCornerRadiusDp = currentState.appCornerRadiusDp,
                content = content,
            )
        }
    }
}

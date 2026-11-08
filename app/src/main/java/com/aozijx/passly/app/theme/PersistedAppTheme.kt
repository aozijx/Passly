package com.aozijx.passly.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent

/** Renders only after the persisted appearance and interface preferences are available. */
@Composable
internal fun PersistedAppTheme(content: @Composable () -> Unit) {
    val viewModel: AppThemeSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentState = state
    when (currentState) {
        RecoverableReadState.Loading -> {
            return
        }

        is RecoverableReadState.Failed -> {
            SettingsReadFailureContent(onRetry = viewModel::retry)
        }

        is RecoverableReadState.Ready -> {
            AppTheme(
                appearance = currentState.value.appearance,
                appCornerRadiusDp = currentState.value.appCornerRadiusDp,
                content = content,
            )
        }
    }
}

package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Renders only after the persisted appearance and interface preferences are available. */
@Composable
internal fun PersistedAppTheme(
    content: @Composable () -> Unit,
) {
    val viewModel: AppThemeSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state as? AppThemeSettingsState.Ready ?: return

    AppTheme(
        appearance = settings.appearance,
        appCornerRadiusDp = settings.appCornerRadiusDp,
        content = content,
    )
}

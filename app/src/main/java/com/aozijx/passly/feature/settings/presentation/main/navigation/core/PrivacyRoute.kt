package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.settings.presentation.security.PrivacySettingsAction
import com.aozijx.passly.feature.settings.presentation.security.PrivacySettingsViewModel
import com.aozijx.passly.feature.settings.presentation.security.PrivacySettingsEffect
import com.aozijx.passly.feature.settings.presentation.security.PrivacySettingsUiState
import com.aozijx.passly.feature.settings.presentation.security.toPrivacySettingsUiModel
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.feature.settings.presentation.ui.security.PrivacyDetail
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage

@Composable
internal fun PrivacyRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: PrivacySettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clearedText = stringResource(com.aozijx.passly.R.string.notice_clipboard_cleared)
    val notClearedText = stringResource(
        com.aozijx.passly.R.string.settings_privacy_clipboard_not_owned
    )
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                PrivacySettingsEffect.ClipboardCleared -> clearedText
                PrivacySettingsEffect.ClipboardNotCleared -> notClearedText
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.PRIVACY.titleRes),
        onBack = onBack
    ) {
        when (val readyState = state) {
            PrivacySettingsUiState.Loading -> Unit
            is PrivacySettingsUiState.Ready -> item {
                PrivacyDetail(
                state = readyState.toPrivacySettingsUiModel(),
                onSecureContentEnabledChange = {
                    viewModel.onAction(PrivacySettingsAction.SetSecureContentEnabled(it))
                },
                onFlipToLockEnabledChange = {
                    viewModel.onAction(PrivacySettingsAction.SetFlipToLockEnabled(it))
                },
                onFlipExitAndClearStackEnabledChange = {
                    viewModel.onAction(
                        PrivacySettingsAction.SetFlipExitAndClearStackEnabled(it)
                    )
                },
                onSensitiveCopyReauthenticationChange = {
                    viewModel.onAction(
                        PrivacySettingsAction.SetSensitiveCopyReauthentication(it)
                    )
                },
                onClipboardClearEnabledChange = {
                    viewModel.onAction(PrivacySettingsAction.SetClipboardClearEnabled(it))
                },
                onClipboardClearDelayChange = {
                    viewModel.onAction(
                        PrivacySettingsAction.SetClipboardClearDelaySeconds(it)
                    )
                },
                onClearClipboard = {
                    viewModel.onAction(PrivacySettingsAction.ClearClipboardNow)
                },
                )
            }
        }
    }
}

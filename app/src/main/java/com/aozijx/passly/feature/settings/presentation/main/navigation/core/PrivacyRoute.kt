package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import com.aozijx.passly.R

@Composable
internal fun PrivacyRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: PrivacySettingsViewModel = hiltViewModel()
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, context, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                val messageRes = when (effect) {
                    PrivacySettingsEffect.ClipboardCleared -> R.string.notice_clipboard_cleared
                    PrivacySettingsEffect.ClipboardNotCleared -> R.string.settings_privacy_clipboard_not_owned
                    is PrivacySettingsEffect.SaveFailed -> effect.message.resourceId
                }
                Toast.makeText(context, resources.getString(messageRes), Toast.LENGTH_SHORT).show()
            }
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

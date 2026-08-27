package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.toInterfaceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.libraryQuickFilterOptions
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.PrivacySettingsEffect
import com.aozijx.passly.presentation.feature.settings.security.toPrivacySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.toSecuritySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.appLanguageFromKey
import com.aozijx.passly.presentation.feature.settings.appearance.toAppearanceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.PrivacyDetail
import com.aozijx.passly.presentation.ui.settings.security.SecurityDetail
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.appearance.AppearanceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.InterfaceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.LibraryQuickFiltersSettingsSection

@Composable
internal fun PrivacyRouteContent(
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?
) {
    val viewModel: PrivacySettingsViewModel = hiltViewModel()
    val state by viewModel.config.collectAsStateWithLifecycle()
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
        item {
            PrivacyDetail(
                state = state.toPrivacySettingsUiModel(),
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

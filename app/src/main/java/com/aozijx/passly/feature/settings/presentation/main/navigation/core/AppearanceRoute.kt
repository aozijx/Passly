package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aozijx.passly.feature.settings.presentation.appearance.AppearanceSettingsAction
import com.aozijx.passly.feature.settings.presentation.appearance.AppearanceSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.appearance.toAppearanceUiModel
import com.aozijx.passly.feature.shell.presentation.theme.LocalAppearanceSettings
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.appearance.AppearanceDetail

@Composable
internal fun AppearanceRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel: AppearanceSettingsViewModel = hiltViewModel()
    val appearance = LocalAppearanceSettings.current
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.APPEARANCE.titleRes),
        onBack = onBack
    ) {
        item {
            AppearanceDetail(
                state = appearance.toAppearanceUiModel(),
                onThemeModeChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetThemeMode(it))
                },
                onDynamicColorChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetDynamicColor(it))
                },
                onThemeKeySelect = {
                    viewModel.onAction(AppearanceSettingsAction.SetThemeKey(it))
                },
                onLanguageChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetLanguage(it))
                },
                onFontFamilyChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetFontFamily(it))
                }
            )
        }
    }
}

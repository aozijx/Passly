package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.AppearanceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.appLanguageFromKey
import com.aozijx.passly.presentation.feature.settings.appearance.toAppearanceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.feature.shell.theme.LocalAppearanceSettings
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.appearance.AppearanceDetail

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
                    viewModel.onAction(AppearanceSettingsAction.SetThemeMode(it.toDomainModel()))
                },
                onDynamicColorChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetDynamicColor(it))
                },
                onThemeKeySelect = {
                    viewModel.onAction(AppearanceSettingsAction.SetThemeKey(it))
                },
                onCanvasTintPercentChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetCanvasTintPercent(it))
                },
                onLanguageChange = {
                    viewModel.onAction(
                        AppearanceSettingsAction.SetLanguage(appLanguageFromKey(it))
                    )
                },
                onFontFamilyChange = {
                    viewModel.onAction(AppearanceSettingsAction.SetFontFamily(it.toDomainModel()))
                }
            )
        }
    }
}

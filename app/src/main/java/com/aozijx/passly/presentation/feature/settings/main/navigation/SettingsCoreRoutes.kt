package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aozijx.passly.presentation.feature.settings.security.toPrivacySettingsUiModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.appLanguageFromKey
import com.aozijx.passly.presentation.feature.settings.appearance.toAppearanceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.PrivacyDetail
import com.aozijx.passly.presentation.feature.settings.security.component.SecurityDetail
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.appearance.AppearanceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.InterfaceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.LibraryQuickFiltersSettingsSection

@Composable
internal fun CoreSettingsRouteContent(
    route: SettingsRoute,
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?
) {
    when (route) {
        SettingsRoute.Security -> {
            val viewModel: SecuritySettingsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.SECURITY.titleRes),
                onBack = onBack
            ) {
                item {
                    SecurityDetail(
                        state = state,
                        isAppPasswordEnabled = settingsState.isAppPasswordEnabled,
                        isBiometricEnabled = state.isBiometricEnabled,
                        onLockTimeoutChange = {
                            viewModel.onAction(SecuritySettingsAction.SetLockTimeout(it))
                        },
                        onAppPasswordClick = {
                            settingsViewModel.onAction(SettingsUiAction.RequestAppPasswordEntry)
                        },
                        onBiometricEnabledChange = { enabled ->
                            viewModel.onAction(
                                SecuritySettingsAction.SetBiometricEnabled(enabled)
                            )
                        },
                        onInvalidateKeyOnBioChangeToggle = { enabled ->
                            viewModel.onAction(
                                SecuritySettingsAction.SetInvalidateKeyOnBiometricChange(enabled)
                            )
                        },
                        onLockOnBackgroundChange = {
                            viewModel.onAction(SecuritySettingsAction.ToggleLockOnBackground(it))
                        }
                    )
                }
            }
        }

        SettingsRoute.Privacy -> {
            val viewModel: PrivacySettingsViewModel = hiltViewModel()
            val state by viewModel.config.collectAsStateWithLifecycle()
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
                        }
                    )
                }
            }
        }

        SettingsRoute.Appearance -> {
            val viewModel: AppearanceSettingsViewModel = hiltViewModel()
            val state by viewModel.config.collectAsStateWithLifecycle()
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.APPEARANCE.titleRes),
                onBack = onBack
            ) {
                item {
                    AppearanceDetail(
                        state = state.toAppearanceUiModel(),
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

        SettingsRoute.Interface -> {
            val viewModel: InterfaceSettingsViewModel = hiltViewModel()
            val state by viewModel.config.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.INTERFACE.titleRes),
                onBack = onBack
            ) {
                item {
                    InterfaceDetail(
                        state = state.toInterfaceUiModel(),
                        onStatusBarAutoHideChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetHideSystemBars(it))
                        },
                        onTopBarCollapsibleChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetTopBarCollapsible(it))
                        },
                        onQuickFilterBarCollapsibleChange = {
                            viewModel.onAction(
                                InterfaceSettingsAction.SetQuickFilterBarCollapsible(
                                    it
                                )
                            )
                        },
                        onOuterCornerRadiusChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetOuterCornerRadius(it))
                        },
                        onInnerCornerRadiusChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetInnerCornerRadius(it))
                        },
                        onGroupItemSpacingChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetGroupItemSpacing(it))
                        },
                        onGroupContentPaddingChange = {
                            viewModel.onAction(InterfaceSettingsAction.SetGroupContentPadding(it))
                        },
                        onEntryHierarchyDisplayModeChange = {
                            viewModel.onAction(
                                InterfaceSettingsAction.SetEntryHierarchyDisplayMode(
                                    it.toDomainModel()
                                )
                            )
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    LibraryQuickFiltersSettingsSection(
                        options = libraryQuickFilterOptions(
                            state.enabledLibraryQuickFilterKeys
                        ),
                        onLibraryQuickFilterToggle = {
                            viewModel.onAction(
                                InterfaceSettingsAction.ToggleVisibleLibraryQuickFilter(
                                    it.toDomainModel()
                                )
                            )
                        }
                    )
                }
            }
        }

        else -> error("Unsupported core settings route: ${route.route}")
    }
}

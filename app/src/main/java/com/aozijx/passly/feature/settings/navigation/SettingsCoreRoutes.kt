package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.appearance.AppearanceDetail
import com.aozijx.passly.feature.settings.appearance.AppearanceUiAction
import com.aozijx.passly.feature.settings.appearance.AppearanceViewModel
import com.aozijx.passly.feature.settings.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.appearance.InterfaceUiAction
import com.aozijx.passly.feature.settings.appearance.InterfaceViewModel
import com.aozijx.passly.feature.settings.apppassword.handleAppPasswordEntryClick
import com.aozijx.passly.feature.settings.internal.SettingsGroup
import com.aozijx.passly.feature.settings.security.PrivacyUiAction
import com.aozijx.passly.feature.settings.security.PrivacyViewModel
import com.aozijx.passly.feature.settings.security.SecurityUiAction
import com.aozijx.passly.feature.settings.security.SecurityViewModel
import com.aozijx.passly.feature.settings.security.handleBiometricToggle
import com.aozijx.passly.feature.settings.security.handleInvalidateKeyToggle
import com.aozijx.passly.feature.settings.security.ui.PrivacyDetail
import com.aozijx.passly.feature.settings.security.ui.SecurityDetail
import com.aozijx.passly.feature.settings.shell.SettingsDetailPlaceholder
import com.aozijx.passly.feature.settings.shell.SettingsMainPage
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.SettingsSecondaryPage

internal fun NavGraphBuilder.registerCoreSettingsRoutes(
    navController: NavHostController,
    context: Context,
    localState: SettingsScreenLocalState,
    settingsViewModel: SettingsViewModel,
    onOuterBack: () -> Unit,
    onGroupClick: (SettingsRoute) -> Unit,
    isTwoPane: Boolean
) {
    composable(
        route = SettingsRoute.Main.route,
        enterTransition = { null },
        exitTransition = { null },
        popEnterTransition = { null },
        popExitTransition = { null }
    ) {
        if (isTwoPane) {
            SettingsDetailPlaceholder()
        } else {
            SettingsMainPage(
                onBack = onOuterBack,
                onGroupClick = onGroupClick
            )
        }
    }

    composable(SettingsRoute.Security.route) {
        val viewModel: SecurityViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        val appPasswordEnabled by viewModel.isAppPasswordEnabled.collectAsStateWithLifecycle()
        val biometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.SECURITY.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                SecurityDetail(
                    state = state,
                    isAppPasswordEnabled = appPasswordEnabled,
                    isBiometricEnabled = biometricEnabled,
                    onLockTimeoutChange = {
                        viewModel.onAction(SecurityUiAction.SetLockTimeout(it))
                    },
                    onAppPasswordClick = {
                        handleAppPasswordEntryClick(
                            context = context,
                            isAppPasswordEnabled = appPasswordEnabled,
                            settingsViewModel = settingsViewModel,
                            onAlreadyEnabled = localState::openAppPasswordActionDialog,
                            onVerified = localState::openSetAppPasswordDialog
                        )
                    },
                    onBiometricEnabledChange = { enabled ->
                        handleBiometricToggle(enabled, viewModel::setBiometricEnabled)
                    },
                    onInvalidateKeyOnBioChangeToggle = { enabled ->
                        handleInvalidateKeyToggle(
                            enabled = enabled,
                            switchPolicy = viewModel::switchKeyInvalidationPolicy
                        )
                    },
                    onLockOnBackgroundChange = {
                        viewModel.onAction(SecurityUiAction.ToggleLockOnBackground(it))
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Privacy.route) {
        val viewModel: PrivacyViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.PRIVACY.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                PrivacyDetail(
                    state = state,
                    onSecureContentEnabledChange = {
                        viewModel.onAction(PrivacyUiAction.SetSecureContentEnabled(it))
                    },
                    onFlipToLockEnabledChange = {
                        viewModel.onAction(PrivacyUiAction.SetFlipToLockEnabled(it))
                    },
                    onFlipExitAndClearStackEnabledChange = {
                        viewModel.onAction(PrivacyUiAction.SetFlipExitAndClearStackEnabled(it))
                    },
                    onSensitiveCopyReauthenticationChange = {
                        viewModel.onAction(
                            PrivacyUiAction.SetSensitiveCopyReauthentication(it)
                        )
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Appearance.route) {
        val viewModel: AppearanceViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.APPEARANCE.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                AppearanceDetail(
                    state = state,
                    onThemeModeChange = {
                        viewModel.onAction(AppearanceUiAction.SetThemeMode(it))
                    },
                    onDynamicColorChange = {
                        viewModel.onAction(AppearanceUiAction.SetDynamicColor(it))
                    },
                    onExpressiveEnabledChange = {
                        viewModel.onAction(AppearanceUiAction.SetExpressiveEnabled(it))
                    },
                    onManualThemeColorSelect = {
                        viewModel.onAction(AppearanceUiAction.SelectManualThemeColor(it))
                    },
                    onLanguageChange = {
                        viewModel.onAction(AppearanceUiAction.SetLanguage(it))
                    },
                    onFontFamilyChange = {
                        viewModel.onAction(AppearanceUiAction.SetFontFamily(it))
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Interface.route) {
        val viewModel: InterfaceViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()

        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.INTERFACE.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                InterfaceDetail(
                    state = state,
                    onStatusBarAutoHideChange = {
                        viewModel.onAction(InterfaceUiAction.SetHideSystemBars(it))
                    },
                    onTopBarCollapsibleChange = {
                        viewModel.onAction(InterfaceUiAction.SetTopBarCollapsible(it))
                    },
                    onTabBarCollapsibleChange = {
                        viewModel.onAction(InterfaceUiAction.SetTabBarCollapsible(it))
                    },
                    onOuterCornerRadiusChange = {
                        viewModel.onAction(InterfaceUiAction.SetOuterCornerRadius(it))
                    },
                    onInnerCornerRadiusChange = {
                        viewModel.onAction(InterfaceUiAction.SetInnerCornerRadius(it))
                    },
                    onGroupItemSpacingChange = {
                        viewModel.onAction(InterfaceUiAction.SetGroupItemSpacing(it))
                    },
                    onGroupContentPaddingChange = {
                        viewModel.onAction(InterfaceUiAction.SetGroupContentPadding(it))
                    },
                    onVisibleVaultTabsChange = {
                        viewModel.onAction(InterfaceUiAction.SetVisibleVaultTabs(it))
                    },
                    onTabBarMaxTabsWithoutScrollChange = {
                        viewModel.onAction(
                            InterfaceUiAction.SetMaxTabsWithoutScroll(it)
                        )
                    },
                    onEntryHierarchyDisplayModeChange = {
                        viewModel.onAction(
                            InterfaceUiAction.SetEntryHierarchyDisplayMode(it)
                        )
                    }
                )
            }
        }
    }
}

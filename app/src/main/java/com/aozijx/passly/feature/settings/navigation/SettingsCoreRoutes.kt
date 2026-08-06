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
import com.aozijx.passly.feature.settings.appearance.AppearanceSettingsAction
import com.aozijx.passly.feature.settings.appearance.AppearanceSettingsViewModel
import com.aozijx.passly.feature.settings.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.appearance.InterfaceSettingsAction
import com.aozijx.passly.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.feature.settings.apppassword.handleAppPasswordEntryClick
import com.aozijx.passly.feature.settings.internal.SettingsGroup
import com.aozijx.passly.feature.settings.security.PrivacySettingsAction
import com.aozijx.passly.feature.settings.security.PrivacySettingsViewModel
import com.aozijx.passly.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.feature.settings.security.SecuritySettingsViewModel
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
        val viewModel: SecuritySettingsViewModel = hiltViewModel()
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
                        viewModel.onAction(SecuritySettingsAction.SetLockTimeout(it))
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
                        viewModel.onAction(SecuritySettingsAction.ToggleLockOnBackground(it))
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Privacy.route) {
        val viewModel: PrivacySettingsViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.PRIVACY.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                PrivacyDetail(
                    state = state,
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

    composable(SettingsRoute.Appearance.route) {
        val viewModel: AppearanceSettingsViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.APPEARANCE.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                AppearanceDetail(
                    state = state,
                    onThemeModeChange = {
                        viewModel.onAction(AppearanceSettingsAction.SetThemeMode(it))
                    },
                    onDynamicColorChange = {
                        viewModel.onAction(AppearanceSettingsAction.SetDynamicColor(it))
                    },
                    onExpressiveEnabledChange = {
                        viewModel.onAction(AppearanceSettingsAction.SetExpressiveEnabled(it))
                    },
                    onManualThemeColorSelect = {
                        viewModel.onAction(AppearanceSettingsAction.SelectManualThemeColor(it))
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

    composable(SettingsRoute.Interface.route) {
        val viewModel: InterfaceSettingsViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()

        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.INTERFACE.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                InterfaceDetail(
                    state = state,
                    onStatusBarAutoHideChange = {
                        viewModel.onAction(InterfaceSettingsAction.SetHideSystemBars(it))
                    },
                    onTopBarCollapsibleChange = {
                        viewModel.onAction(InterfaceSettingsAction.SetTopBarCollapsible(it))
                    },
                    onTabBarCollapsibleChange = {
                        viewModel.onAction(InterfaceSettingsAction.SetTabBarCollapsible(it))
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
                    onVisibleVaultTabsChange = {
                        viewModel.onAction(InterfaceSettingsAction.SetVisibleVaultTabs(it))
                    },
                    onTabBarMaxTabsWithoutScrollChange = {
                        viewModel.onAction(
                            InterfaceSettingsAction.SetMaxTabsWithoutScroll(it)
                        )
                    },
                    onEntryHierarchyDisplayModeChange = {
                        viewModel.onAction(
                            InterfaceSettingsAction.SetEntryHierarchyDisplayMode(it)
                        )
                    }
                )
            }
        }
    }
}

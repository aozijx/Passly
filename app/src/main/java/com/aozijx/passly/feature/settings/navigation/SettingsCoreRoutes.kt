package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.feature.settings.appearance.AppearanceDetail
import com.aozijx.passly.feature.settings.appearance.AppearanceUiAction
import com.aozijx.passly.feature.settings.appearance.AppearanceViewModel
import com.aozijx.passly.feature.settings.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.appearance.InterfaceUiAction
import com.aozijx.passly.feature.settings.appearance.InterfaceViewModel
import com.aozijx.passly.feature.settings.apppassword.handleAppPasswordEntryClick
import com.aozijx.passly.feature.settings.security.PrivacyUiAction
import com.aozijx.passly.feature.settings.security.PrivacyViewModel
import com.aozijx.passly.feature.settings.security.SecurityUiAction
import com.aozijx.passly.feature.settings.security.SecurityViewModel
import com.aozijx.passly.feature.settings.security.handleInvalidateKeyToggle
import com.aozijx.passly.feature.settings.security.ui.PrivacyDetail
import com.aozijx.passly.feature.settings.security.ui.SecurityDetail
import com.aozijx.passly.feature.settings.shell.SettingsMainPage
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.SettingsSecondaryPage
import com.aozijx.passly.ui.common.FragmentActivityBiometricLauncher

internal fun NavGraphBuilder.registerCoreSettingsRoutes(
    navController: NavHostController,
    context: Context,
    localState: SettingsScreenLocalState,
    onUpdateInteraction: () -> Unit,
    onOuterBack: () -> Unit,
    authDecryptTitle: String,
    setAppPasswordSubtitle: String
) {
    composable(
        route = SettingsRoute.Main.route,
        enterTransition = { null },
        exitTransition = { null },
        popEnterTransition = { null },
        popExitTransition = { null }
    ) {
        SettingsMainPage(
            onBack = onOuterBack,
            onUpdateInteraction = onUpdateInteraction,
            onGroupClick = { navController.navigate(it.route) }
        )
    }

    composable(SettingsRoute.Security.route) {
        val viewModel: SecurityViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        val appPasswordEnabled by viewModel.isAppPasswordEnabled.collectAsStateWithLifecycle()

        SettingsSecondaryPage(
            title = "安全设置",
            onBack = { navController.popBackStack() }
        ) {
            item {
                SecurityDetail(
                    state = state,
                    isAppPasswordEnabled = appPasswordEnabled,
                    onLockTimeoutChange = {
                        viewModel.onAction(SecurityUiAction.SetLockTimeout(it))
                    },
                    onAppPasswordClick = {
                        val launcher = (context as? FragmentActivity)
                            ?.let(::FragmentActivityBiometricLauncher)
                        handleAppPasswordEntryClick(
                            context = context,
                            launcher = launcher,
                            isAppPasswordEnabled = appPasswordEnabled,
                            authGateway = viewModel.authGateway,
                            title = authDecryptTitle,
                            subtitle = setAppPasswordSubtitle,
                            onAlreadyEnabled = localState::openAppPasswordActionDialog,
                            onVerified = localState::openSetAppPasswordDialog
                        )
                    },
                    onInvalidateKeyOnBioChangeToggle = { enabled ->
                        val launcher = (context as? FragmentActivity)
                            ?.let(::FragmentActivityBiometricLauncher)
                        handleInvalidateKeyToggle(
                            context = context,
                            launcher = launcher,
                            enabled = enabled,
                            switchPolicy = viewModel::switchKeyInvalidationPolicy
                        )
                    },
                    onLockOnBackgroundChange = {
                        viewModel.onAction(SecurityUiAction.ToggleLockOnBackground(it))
                    },
                    onClipboardClearToastsChange = {
                        viewModel.onAction(SecurityUiAction.ToggleClipboardClearToasts(it))
                    },
                    onAppCloseToastsChange = {
                        viewModel.onAction(SecurityUiAction.ToggleAppCloseToasts(it))
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Privacy.route) {
        val viewModel: PrivacyViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(title = "隐私设置", onBack = { navController.popBackStack() }) {
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
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Appearance.route) {
        val viewModel: AppearanceViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(title = "外观设置", onBack = { navController.popBackStack() }) {
            item {
                AppearanceDetail(
                    state = state,
                    onDarkModeChange = {
                        viewModel.onAction(AppearanceUiAction.SetDarkMode(it))
                    },
                    onDynamicColorChange = {
                        viewModel.onAction(AppearanceUiAction.SetDynamicColor(it))
                    },
                    onThemeColorChange = {
                        viewModel.onAction(AppearanceUiAction.SetThemeColor(it))
                    }
                )
            }
        }
    }

    composable(SettingsRoute.Interface.route) {
        val viewModel: InterfaceViewModel = hiltViewModel()
        val state by viewModel.config.collectAsStateWithLifecycle()
        val effectiveStyle = VaultCardStyle.normalizeGlobalStyle(state.cardStyle)
        val loginStyle = state.perTypeMap[EntryType.LOGIN.ordinal] ?: VaultCardStyle.DEFAULT

        LaunchedEffect(state.cardStyle) {
            if (state.cardStyle != effectiveStyle) {
                viewModel.onAction(InterfaceUiAction.SetLoginCardStyle(effectiveStyle))
            }
        }

        SettingsSecondaryPage(title = "界面设置", onBack = { navController.popBackStack() }) {
            item {
                InterfaceDetail(
                    state = state,
                    availableCardStyles = VaultCardStyle.entries.toList(),
                    loginSelectedStyle = loginStyle,
                    onStatusBarAutoHideChange = {
                        viewModel.onAction(InterfaceUiAction.SetStatusBarAutoHide(it))
                    },
                    onTopBarCollapsibleChange = {
                        viewModel.onAction(InterfaceUiAction.SetTopBarCollapsible(it))
                    },
                    onTabBarCollapsibleChange = {
                        viewModel.onAction(InterfaceUiAction.SetTabBarCollapsible(it))
                    },
                    onLoginStyleSelected = {
                        viewModel.onAction(InterfaceUiAction.SetLoginCardStyle(it))
                    },
                    onVisibleVaultTabsChange = {
                        viewModel.onAction(InterfaceUiAction.SetVisibleVaultTabs(it))
                    },
                    onTabBarMaxTabsWithoutScrollChange = {
                        viewModel.onAction(
                            InterfaceUiAction.SetTabBarMaxTabsWithoutScroll(it)
                        )
                    }
                )
            }
        }
    }
}

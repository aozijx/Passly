package com.aozijx.passly.ui.features.settings.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.ui.features.settings.SettingsViewModel
import com.aozijx.passly.ui.features.settings.appearance.AppearanceDetail
import com.aozijx.passly.ui.features.settings.appearance.AppearanceUiAction
import com.aozijx.passly.ui.features.settings.appearance.AppearanceViewModel
import com.aozijx.passly.ui.features.settings.appearance.InterfaceDetail
import com.aozijx.passly.ui.features.settings.appearance.InterfaceUiAction
import com.aozijx.passly.ui.features.settings.appearance.InterfaceViewModel
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordAction
import com.aozijx.passly.ui.features.settings.apppassword.handleAppPasswordAction
import com.aozijx.passly.ui.features.settings.apppassword.handleAppPasswordEntryClick
import com.aozijx.passly.ui.features.settings.data.DataManagementDetail
import com.aozijx.passly.ui.features.settings.data.DataUiAction
import com.aozijx.passly.ui.features.settings.data.DataViewModel
import com.aozijx.passly.ui.features.settings.data.handleBackupPathPicked
import com.aozijx.passly.ui.features.settings.general.GeneralDetail
import com.aozijx.passly.ui.features.settings.interaction.InteractionDetail
import com.aozijx.passly.ui.features.settings.interaction.InteractionUiAction
import com.aozijx.passly.ui.features.settings.interaction.InteractionViewModel
import com.aozijx.passly.ui.features.settings.security.PrivacyDetail
import com.aozijx.passly.ui.features.settings.security.PrivacyUiAction
import com.aozijx.passly.ui.features.settings.security.PrivacyViewModel
import com.aozijx.passly.ui.features.settings.security.SecurityDetail
import com.aozijx.passly.ui.features.settings.security.SecurityUiAction
import com.aozijx.passly.ui.features.settings.security.SecurityViewModel
import com.aozijx.passly.ui.features.settings.security.handleInvalidateKeyToggle
import com.aozijx.passly.ui.features.settings.shell.SettingsMainPage
import com.aozijx.passly.ui.features.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.ui.features.settings.shell.SettingsSecondaryPage
import com.aozijx.passly.ui.features.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.ui.features.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.ui.features.settings.shell.rememberSettingsScreenLocalState

@Composable
fun SettingsNavGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit,
    onOuterBack: () -> Unit
) {
    val localState = rememberSettingsScreenLocalState()
    val context = LocalContext.current

    // 对话框层使用的 ViewModel（NavGraph 级别，路由间共享）
    val interactionViewModel: InteractionViewModel = hiltViewModel()
    val interactionState by interactionViewModel.config.collectAsStateWithLifecycle()
    val dataViewModel: DataViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()
    val interfaceViewModel: InterfaceViewModel = hiltViewModel()
    val interfaceState by interfaceViewModel.config.collectAsStateWithLifecycle()

    val authDecryptTitle = stringResource(R.string.vault_auth_decrypt_title)
    val setAppPasswordSubtitle = stringResource(R.string.settings_auth_before_set_app_password)
    val authFailedMsg = stringResource(R.string.vault_auth_failed)

    fun submitAppPasswordAction(action: AppPasswordAction) {
        handleAppPasswordAction(
            context = context,
            action = action,
            currentPassword = localState.appPasswordCurrent,
            newPassword = localState.appPasswordNew,
            confirmPassword = localState.appPasswordConfirm,
            authGateway = settingsViewModel.authGateway,
            onSuccess = localState::onAppPasswordSuccess
        )
    }

    val backupPathLabel = remember(dataState.directoryUri) {
        localState.backupPathLabel(dataState.directoryUri)
    }
    val lastExportFileLabel = remember(dataState.lastExportFileName) {
        localState.lastExportFileLabel(dataState.lastExportFileName)
    }
    val availableCardStyles = remember { VaultCardStyle.entries.toList() }
    val effectiveCardStyle = VaultCardStyle.normalizeGlobalStyle(interfaceState.cardStyle)
    val passwordSelectedStyle =
        interfaceState.perTypeMap[EntryType.PASSWORD.value] ?: VaultCardStyle.DEFAULT
    val totpSelectedStyle =
        interfaceState.perTypeMap[EntryType.TOTP.value] ?: VaultCardStyle.DEFAULT

    LaunchedEffect(interfaceState.cardStyle) {
        if (interfaceState.cardStyle != effectiveCardStyle)
            interfaceViewModel.onAction(InterfaceUiAction.SetPasswordCardStyle(effectiveCardStyle))
    }

    LaunchedEffect(dataState.backupMessage) {
        dataState.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            dataViewModel.onAction(DataUiAction.ClearBackupMessage)
        }
    }

    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // 主页面（禁用共享动画，避免从外部进入时也有滑动效果）
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
                onGroupClick = { route ->
                    navController.navigate(route.route)
                }
            )
        }

        // 安全设置
        composable(SettingsRoute.Security.route) {
            val vm: SecurityViewModel = hiltViewModel()
            val state by vm.config.collectAsStateWithLifecycle()
            val appPwdEnabled by vm.isAppPasswordEnabled.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = "安全设置",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    SecurityDetail(
                        state = state,
                        isAppPasswordEnabled = appPwdEnabled,
                        onLockTimeoutChange = { vm.onAction(SecurityUiAction.SetLockTimeout(it)) },
                        onAppPasswordClick = {
                            handleAppPasswordEntryClick(
                                context = context,
                                activity = context as? FragmentActivity,
                                isAppPasswordEnabled = appPwdEnabled,
                                authGateway = vm.authGateway,
                                title = authDecryptTitle,
                                subtitle = setAppPasswordSubtitle,
                                authFailedMsg = authFailedMsg,
                                onAlreadyEnabled = localState::openAppPasswordActionDialog,
                                onVerified = localState::openSetAppPasswordDialog
                            )
                        },
                        onPasswordPreferredAuthFirstChange = {
                            vm.onAction(SecurityUiAction.SetPasswordPreferredAuthFirst(it))
                        },
                        onDeviceCredentialFallbackToggleRequested = { enabled ->
                            if (enabled && !state.isDeviceCredentialFallbackEnabled) {
                                localState.openDeviceCredentialFallbackWarningDialog()
                            } else {
                                vm.onAction(SecurityUiAction.ToggleDeviceCredentialFallback(enabled))
                            }
                        },
                        onInvalidateKeyOnBioChangeToggle = { enabled ->
                            handleInvalidateKeyToggle(
                                context = context,
                                activity = context as? FragmentActivity,
                                enabled = enabled,
                                switchPolicy = vm::switchKeyInvalidationPolicy
                            )
                        }
                    )
                }
            }
        }

        // 隐私设置
        composable(SettingsRoute.Privacy.route) {
            val vm: PrivacyViewModel = hiltViewModel()
            val state by vm.config.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = "隐私设置",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    PrivacyDetail(
                        state = state,
                        onSecureContentEnabledChange = {
                            vm.onAction(PrivacyUiAction.SetSecureContentEnabled(it))
                        },
                        onFlipToLockEnabledChange = {
                            vm.onAction(PrivacyUiAction.SetFlipToLockEnabled(it))
                        },
                        onFlipExitAndClearStackEnabledChange = {
                            vm.onAction(PrivacyUiAction.SetFlipExitAndClearStackEnabled(it))
                        }
                    )
                }
            }
        }

        // 外观设置
        composable(SettingsRoute.Appearance.route) {
            val vm: AppearanceViewModel = hiltViewModel()
            val state by vm.config.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = "外观设置",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    AppearanceDetail(
                        state = state,
                        onDarkModeChange = { vm.onAction(AppearanceUiAction.SetDarkMode(it)) },
                        onDynamicColorChange = { vm.onAction(AppearanceUiAction.SetDynamicColor(it)) }
                    )
                }
            }
        }

        // 界面设置
        composable(SettingsRoute.Interface.route) {
            val state by interfaceViewModel.config.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = "界面设置",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    InterfaceDetail(
                        state = state,
                        availableCardStyles = availableCardStyles,
                        passwordSelectedStyle = passwordSelectedStyle,
                        totpSelectedStyle = totpSelectedStyle,
                        onStatusBarAutoHideChange = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetStatusBarAutoHide(it))
                        },
                        onTopBarCollapsibleChange = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetTopBarCollapsible(it))
                        },
                        onTabBarCollapsibleChange = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetTabBarCollapsible(it))
                        },
                        onPasswordStyleSelected = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetPasswordCardStyle(it))
                        },
                        onTotpStyleSelected = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetTotpCardStyle(it))
                        },
                        onVisibleVaultTabsChange = {
                            interfaceViewModel.onAction(InterfaceUiAction.SetVisibleVaultTabs(it))
                        },
                        onTabBarMaxTabsWithoutScrollChange = {
                            interfaceViewModel.onAction(
                                InterfaceUiAction.SetTabBarMaxTabsWithoutScroll(
                                    it
                                )
                            )
                        }
                    )
                }
            }
        }

        // 交互设置
        composable(SettingsRoute.Interaction.route) {
            val state by interactionViewModel.config.collectAsStateWithLifecycle()

            SettingsSecondaryPage(
                title = "交互与操作",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    InteractionDetail(
                        state = state,
                        onSwipeEnabledChange = {
                            interactionViewModel.onAction(InteractionUiAction.SetSwipeEnabled(it))
                        },
                        onLeftSwipeActionClick = localState::openLeftActionDialog,
                        onRightSwipeActionClick = localState::openRightActionDialog,
                        onToggleAutofillUiMode = {
                            interactionViewModel.onAction(InteractionUiAction.ToggleAutofillUiMode)
                        }
                    )
                }
            }
        }

        // 数据管理
        composable(SettingsRoute.DataManagement.route) {
            val state by dataViewModel.config.collectAsStateWithLifecycle()

            val backupPathPicker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    handleBackupPathPicked(context, uri) { resolvedUri ->
                        dataViewModel.onAction(DataUiAction.SetBackupDirectoryUri(resolvedUri))
                    }
                }

            SettingsSecondaryPage(
                title = "数据管理",
                onBack = { navController.popBackStack() }
            ) {
                item {
                    DataManagementDetail(
                        state = state,
                        backupPathLabel = backupPathLabel,
                        lastExportFileLabel = lastExportFileLabel,
                        onAutoDownloadIconsChange = {
                            dataViewModel.onAction(DataUiAction.SetAutoDownloadIcons(it))
                        },
                        onPickBackupPath = {
                            backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri())
                        },
                        onTestBackupWrite = {
                            dataViewModel.testBackupDirectoryWritePermission(state.directoryUri)
                        },
                        onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                        else localState::openClearBackupDirConfirmDialog
                    )
                }
            }
        }

        // 通用
        composable(SettingsRoute.General.route) {
            SettingsSecondaryPage(
                title = "通用",
                onBack = { navController.popBackStack() }
            ) {
                item { GeneralDetail() }
            }
        }
    }

    // 对话框层
    SettingsScreenDialogsHost(
        state = buildSettingsDialogsState(
            localState = localState,
            swipeLeftAction = interactionState.swipeLeftAction,
            swipeRightAction = interactionState.swipeRightAction,
            backupDirectoryUri = dataState.directoryUri,
            context = context
        ),
        actions = buildSettingsDialogsActions(
            localState = localState,
            onSetSwipeRightAction = settingsViewModel::setSwipeRightAction,
            onSetSwipeLeftAction = settingsViewModel::setSwipeLeftAction,
            onSetDeviceCredentialFallback = settingsViewModel::setDeviceCredentialFallbackEnabled,
            submitAppPasswordAction = ::submitAppPasswordAction,
            onClearBackupDirectory = { dataViewModel.onAction(DataUiAction.ClearBackupDirectory) }
        )
    )
}
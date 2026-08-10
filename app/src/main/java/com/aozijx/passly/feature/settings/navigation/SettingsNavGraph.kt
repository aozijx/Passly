package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.apppassword.AppPasswordAction
import com.aozijx.passly.feature.settings.apppassword.validateAndSendAppPasswordAction
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsAction
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.shell.SettingsMainPage
import com.aozijx.passly.feature.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.feature.settings.shell.rememberSettingsScreenLocalState
import kotlinx.coroutines.launch

/**
 * 使用 Material3 [ListDetailPaneScaffold] 实现自适应双栏设置页。
 *
 * - 窄屏：单栏模式，列表与详情通过 pane 切换展示
 * - 宽屏（MEDIUM 及以上）：双栏模式，左侧列表 + 右侧详情
 * - 自动处理窗口大小变化、预测返回手势和 pane 动画
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsNavGraph(
    settingsViewModel: SettingsViewModel,
    onOuterBack: () -> Unit
) {
    val navigator = rememberListDetailPaneScaffoldNavigator()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val localState = rememberSettingsScreenLocalState()
    val context = LocalContext.current
    val interactionViewModel: InteractionSettingsViewModel = hiltViewModel()
    val interactionState by interactionViewModel.config.collectAsStateWithLifecycle()
    val dataViewModel: DataManagementSettingsViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navigateBackToList: () -> Unit = {
        navController.popBackStack(SettingsRoute.Main.route, inclusive = false)
        scope.launch { navigator.navigateBack() }
    }

    fun submitAppPasswordAction(action: AppPasswordAction) {
        validateAndSendAppPasswordAction(
            context = context,
            action = action,
            currentPassword = localState.appPasswordCurrent,
            newPassword = localState.appPasswordNew,
            confirmPassword = localState.appPasswordConfirm,
            settingsViewModel = settingsViewModel
        )
    }

    // 单栏模式下，详情页返回列表：同步重置 NavHost 返回栈
    BackHandler(enabled = navigator.canNavigateBack()) {
        navigateBackToList()
    }

    LaunchedEffect(Unit) {
        settingsViewModel.effects.collect { effect ->
            val message = when (effect) {
                is SettingsEffect.ShowError -> effect.message
                is SettingsEffect.SettingsSaved -> "设置已保存"
                is SettingsEffect.DatabaseCleared -> "保险库数据库已永久清除"
                is SettingsEffect.AppPasswordSet -> {
                    localState.onAppPasswordSuccess(AppPasswordAction.SET)
                    context.getString(R.string.settings_auth_password_set_success)
                }

                is SettingsEffect.AppPasswordChanged -> {
                    localState.onAppPasswordSuccess(AppPasswordAction.CHANGE)
                    context.getString(R.string.settings_auth_password_change_success)
                }

                is SettingsEffect.AppPasswordDisabled -> {
                    localState.onAppPasswordSuccess(AppPasswordAction.DISABLE)
                    context.getString(R.string.settings_auth_password_disabled)
                }

                is SettingsEffect.AppPasswordError -> effect.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            SettingsMainPage(
                onBack = onOuterBack,
                onGroupClick = { route ->
                    navController.navigate(route.route) {
                        popUpTo(SettingsRoute.Main.route) { inclusive = false }
                        launchSingleTop = true
                    }
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                },
                selectedRouteKey = currentRoute
            )
        },
        detailPane = {
            SettingsNavHost(
                navController = navController,
                context = context,
                localState = localState,
                settingsViewModel = settingsViewModel,
                interactionViewModel = interactionViewModel,
                dataViewModel = dataViewModel,
                settingsState = settingsState,
                onBack = navigateBackToList
            )
        }
    )

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
            onSetSwipeRightAction = {
                settingsViewModel.handleIntent(SettingsIntent.SetSwipeRightAction(it))
            },
            onSetSwipeLeftAction = {
                settingsViewModel.handleIntent(SettingsIntent.SetSwipeLeftAction(it))
            },
            submitAppPasswordAction = ::submitAppPasswordAction,
            onClearBackupDirectory = {
                dataViewModel.onAction(DataManagementSettingsAction.ClearBackupDirectory)
            }
        )
    )
}

@Composable
private fun SettingsNavHost(
    navController: NavHostController,
    context: Context,
    localState: SettingsScreenLocalState,
    settingsViewModel: SettingsViewModel,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsState: SettingsUiState,
    onBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route
    ) {
        registerCoreSettingsRoutes(
            context = context,
            localState = localState,
            settingsViewModel = settingsViewModel,
            onBack = onBack
        )
        registerDataSettingsRoutes(
            context = context,
            localState = localState,
            interactionViewModel = interactionViewModel,
            dataViewModel = dataViewModel,
            settingsViewModel = settingsViewModel,
            settingsState = settingsState,
            onBack = onBack
        )
    }
}

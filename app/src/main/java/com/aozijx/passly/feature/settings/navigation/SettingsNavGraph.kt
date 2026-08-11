package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.apppassword.AppPasswordAction
import com.aozijx.passly.feature.settings.apppassword.validateAndSendAppPasswordAction
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsAction
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.shell.SettingsDetailPlaceholder
import com.aozijx.passly.feature.settings.shell.SettingsMainPage
import com.aozijx.passly.feature.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.feature.settings.shell.rememberSettingsScreenLocalState
import kotlinx.coroutines.launch

/**
 * 使用单一 Adaptive Navigator 实现自适应设置页。
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
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsRoute>()
    val scope = rememberCoroutineScope()
    val localState = rememberSettingsScreenLocalState()
    val context = LocalContext.current
    val interactionViewModel: InteractionSettingsViewModel = hiltViewModel()
    val interactionState by interactionViewModel.uiState.collectAsStateWithLifecycle()
    val dataViewModel: DataManagementSettingsViewModel = hiltViewModel()
    val dataState by dataViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val backBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
    val isSinglePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    var selectedRoute by rememberSaveable {
        mutableStateOf(navigator.currentDestination?.contentKey)
    }
    val selectedRouteKey = if (isSinglePane) null else selectedRoute?.route
    val motionScheme = MaterialTheme.motionScheme
    val navigateBack: () -> Unit = {
        scope.launch { navigator.navigateBack(backBehavior) }
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

    LaunchedEffect(Unit) {
        settingsViewModel.effects.collect { effect ->
            // 副作用
            when (effect) {
                is SettingsEffect.AppPasswordSet -> localState.onAppPasswordSuccess(
                    AppPasswordAction.SET
                )

                is SettingsEffect.AppPasswordChanged -> localState.onAppPasswordSuccess(
                    AppPasswordAction.CHANGE
                )

                is SettingsEffect.AppPasswordDisabled -> localState.onAppPasswordSuccess(
                    AppPasswordAction.DISABLE
                )

                is SettingsEffect.AppPasswordEntryAuthorized -> {
                    if (effect.alreadyEnabled) {
                        localState.openAppPasswordActionDialog()
                    } else {
                        localState.openSetAppPasswordDialog()
                    }
                }

                else -> {}
            }
            effect.toMessage(context)?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        defaultBackBehavior = backBehavior,
        listPane = {
            AnimatedPane(
                modifier = Modifier.zIndex(0f),
                enterTransition = if (isSinglePane) {
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = motionScheme.defaultSpatialSpec()
                    )
                } else {
                    EnterTransition.None
                },
                exitTransition = if (isSinglePane) {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = motionScheme.defaultSpatialSpec()
                    )
                } else {
                    ExitTransition.None
                }
            ) {
                SettingsMainPage(
                    onBack = onOuterBack,
                    onGroupClick = { route ->
                        selectedRoute = route
                        if (
                            isSinglePane ||
                            navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.Detail
                        ) {
                            scope.launch {
                                navigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail,
                                    contentKey = route
                                )
                            }
                        }
                    },
                    selectedRouteKey = selectedRouteKey
                )
            }
        },
        detailPane = {
            AnimatedPane(
                // 推入和返回期间详情始终覆盖列表，避免 Scaffold 提前切换目标层级后
                // 从退出页面边缘露出底层的分栏间隙。
                modifier = Modifier.zIndex(1f),
                enterTransition = if (isSinglePane) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = motionScheme.defaultSpatialSpec()
                    )
                } else {
                    EnterTransition.None
                },
                exitTransition = if (isSinglePane) {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = motionScheme.defaultSpatialSpec()
                    )
                } else {
                    ExitTransition.None
                }
            ) {
                SettingsDetailContent(
                    route = selectedRoute,
                    context = context,
                    localState = localState,
                    settingsViewModel = settingsViewModel,
                    interactionViewModel = interactionViewModel,
                    dataViewModel = dataViewModel,
                    settingsState = settingsState,
                    onBack = if (isSinglePane) navigateBack else null
                )
            }
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
private fun SettingsDetailContent(
    route: SettingsRoute?,
    context: Context,
    localState: SettingsScreenLocalState,
    settingsViewModel: SettingsViewModel,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    when (route) {
        null,
        SettingsRoute.Main -> SettingsDetailPlaceholder()

        SettingsRoute.Security,
        SettingsRoute.Privacy,
        SettingsRoute.Appearance,
        SettingsRoute.Interface -> {
            CoreSettingsRouteContent(
                route = route,
                localState = localState,
                settingsViewModel = settingsViewModel,
                onBack = onBack
            )
        }

        SettingsRoute.Interaction,
        SettingsRoute.DataManagement,
        SettingsRoute.BackupRestore,
        SettingsRoute.RecoveryCode,
        SettingsRoute.General,
        SettingsRoute.Notifications -> {
            DataSettingsRouteContent(
                route = route,
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
}

package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogsState
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.feature.database.reset.DatabaseResetOverlay
import com.aozijx.passly.presentation.ui.settings.main.SettingsMainPage
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenDialogsHost
import com.aozijx.passly.presentation.ui.settings.main.rememberSettingsScreenLocalState
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
    onOpenTrash: () -> Unit,
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
    val mainListState = rememberLazyGridState()

    val backBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
    val isSinglePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val selectedRoute = navigator.currentDestination?.contentKey
    var retainedDetailRoute by remember { mutableStateOf<SettingsRoute?>(null) }
    LaunchedEffect(selectedRoute) {
        if (selectedRoute != null) retainedDetailRoute = selectedRoute
    }
    val renderedDetailRoute = resolveSettingsDetailRoute(
        isSinglePane = isSinglePane,
        navigatorRoute = selectedRoute,
        retainedDetailRoute = retainedDetailRoute,
    )
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
                is SettingsEffect.AppPasswordSet -> localState.onAppPasswordSuccess()

                is SettingsEffect.AppPasswordChanged -> localState.onAppPasswordSuccess()

                is SettingsEffect.AppPasswordDisabled -> localState.onAppPasswordSuccess()

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

    if (isSinglePane) {
        SettingsSinglePane(
            navigator = navigator,
            backBehavior = backBehavior,
            currentPage = if (selectedRoute == null) {
                SettingsSinglePanePage.List
            } else {
                SettingsSinglePanePage.Detail
            },
            listContent = {
                SettingsMainPage(
                    onBack = onOuterBack,
                    onGroupClick = { routeKey ->
                        val route = SettingsRoute.fromRouteKey(routeKey) ?: return@SettingsMainPage
                        if (navigator.currentDestination?.contentKey != route) {
                            scope.launch {
                                navigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail,
                                    contentKey = route
                                )
                            }
                        }
                    },
                    selectedRouteKey = null,
                    listState = mainListState,
                )
            },
            detailContent = {
                SettingsDetailRouteRegistry(
                    route = renderedDetailRoute,
                    context = context,
                    localState = localState,
                    settingsViewModel = settingsViewModel,
                    interactionViewModel = interactionViewModel,
                    dataViewModel = dataViewModel,
                    settingsState = settingsState,
                    onOpenTrash = onOpenTrash,
                    onBack = navigateBack,
                )
            },
        )
    } else {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            defaultBackBehavior = backBehavior,
            listPane = {
                AnimatedPane(
                    enterTransition = EnterTransition.None,
                    exitTransition = ExitTransition.None,
                ) {
                    SettingsMainPage(
                        onBack = onOuterBack,
                        onGroupClick = { routeKey ->
                            val route = SettingsRoute.fromRouteKey(routeKey)
                                ?: return@SettingsMainPage
                            if (navigator.currentDestination?.contentKey != route) {
                                scope.launch {
                                    navigator.navigateTo(
                                        pane = ListDetailPaneScaffoldRole.Detail,
                                        contentKey = route,
                                    )
                                }
                            }
                        },
                        selectedRouteKey = selectedRoute?.route,
                        listState = mainListState,
                    )
                }
            },
            detailPane = {
                AnimatedPane(
                    enterTransition = EnterTransition.None,
                    exitTransition = ExitTransition.None,
                ) {
                    // 详情内容路由切换：利用 AnimatedContent 的可中断/重定向机制——
                    // 动画播放中若目标路由再次变化，动画立即重定向到新目标，配合 spring
                    // 弹簧动画平滑衔接，快速连续点击多个设置项也不会跳变或卡顿。
                    AnimatedContent(
                        targetState = renderedDetailRoute,
                        transitionSpec = {
                            val enter = fadeIn(
                                animationSpec = routeFadeIn
                            ) + slideInHorizontally(
                                initialOffsetX = { it / 5 },
                                animationSpec = routeSlide
                            )
                            val exit = fadeOut(
                                animationSpec = routeFadeOut
                            ) + slideOutHorizontally(
                                targetOffsetX = { -it / 5 },
                                animationSpec = routeSlide
                            )
                            enter togetherWith exit using SizeTransform(clip = false)
                        },
                        label = "settingsDetail"
                    ) { route ->
                        SettingsDetailRouteRegistry(
                            route = route,
                            context = context,
                            localState = localState,
                            settingsViewModel = settingsViewModel,
                            interactionViewModel = interactionViewModel,
                            dataViewModel = dataViewModel,
                            settingsState = settingsState,
                            onOpenTrash = onOpenTrash,
                            onBack = null,
                        )
                    }
                }
            },
        )
    }

    SettingsScreenDialogsHost(
        state = buildSettingsDialogsState(
            localState = localState,
            swipeLeftAction = interactionState.swipeLeftAction,
            swipeRightAction = interactionState.swipeRightAction,
        ),
        onEvent = buildSettingsDialogEventHandler(
            localState = localState,
            backupDirectoryUri = dataState.directoryUri,
            context = context,
            onSetSwipeRightAction = {
                settingsViewModel.onAction(SettingsUiAction.SetSwipeRightAction(it))
            },
            onSetSwipeLeftAction = {
                settingsViewModel.onAction(SettingsUiAction.SetSwipeLeftAction(it))
            },
            submitAppPasswordAction = ::submitAppPasswordAction,
            onClearBackupDirectory = {
                dataViewModel.onAction(DataManagementSettingsUiAction.ClearBackupDirectory)
            }
        )
    )

    if (localState.showDatabaseResetSheet) {
        DatabaseResetOverlay(onDismiss = localState::dismissDatabaseResetSheet)
    }
}

/**
 * 详情内容路由切换动画（spring 弹簧）。
 *
 * spring 动画天然可中断：动画播放中若目标路由再次变化，会立即从当前状态
 * 重定向到新目标，而不是重启整段动画。快速连续切换多个设置项时，
 * 内容平滑追向最新目标，避免跳变与掉帧。
 */
private val routeSlide = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private val routeFadeIn = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private val routeFadeOut = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

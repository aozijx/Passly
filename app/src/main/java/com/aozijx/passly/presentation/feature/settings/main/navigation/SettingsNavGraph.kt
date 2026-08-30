package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldPredictiveBackHandler
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogsState
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.SettingsDetailPlaceholder
import com.aozijx.passly.presentation.ui.settings.main.SettingsMainPage
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenDialogsHost
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
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
    onOpenDatabaseRecovery: () -> Unit,
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
                    selectedRouteKey = null
                )
            },
            detailContent = {
                SettingsDetailContent(
                    route = renderedDetailRoute,
                    context = context,
                    localState = localState,
                    settingsViewModel = settingsViewModel,
                    interactionViewModel = interactionViewModel,
                    dataViewModel = dataViewModel,
                    settingsState = settingsState,
                    onOpenTrash = onOpenTrash,
                    onOpenDatabaseRecovery = onOpenDatabaseRecovery,
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
                        SettingsDetailContent(
                            route = route,
                            context = context,
                            localState = localState,
                            settingsViewModel = settingsViewModel,
                            interactionViewModel = interactionViewModel,
                            dataViewModel = dataViewModel,
                            settingsState = settingsState,
                            onOpenTrash = onOpenTrash,
                            onOpenDatabaseRecovery = onOpenDatabaseRecovery,
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
}

internal enum class SettingsSinglePanePage {
    List,
    Detail,
}

internal enum class SettingsSinglePaneTargetLayer(val zIndex: Float) {
    Background(-1f),
    Default(0f),
    Foreground(1f),
}

internal fun resolveSettingsSinglePaneTargetLayer(
    initial: SettingsSinglePanePage,
    target: SettingsSinglePanePage,
): SettingsSinglePaneTargetLayer = when {
    initial == target -> SettingsSinglePaneTargetLayer.Default
    target == SettingsSinglePanePage.Detail -> SettingsSinglePaneTargetLayer.Foreground
    else -> SettingsSinglePaneTargetLayer.Background
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsSinglePane(
    navigator: ThreePaneScaffoldNavigator<SettingsRoute>,
    backBehavior: BackNavigationBehavior,
    currentPage: SettingsSinglePanePage,
    listContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
) {
    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = backBehavior,
    )

    val motionScheme = MaterialTheme.motionScheme
    val visualState = remember { SeekableTransitionState(currentPage) }
    val visualTransition = rememberTransition(visualState, label = "settingsSinglePane")
    val scaffoldState = navigator.scaffoldState

    LaunchedEffect(scaffoldState) {
        snapshotFlow {
            scaffoldState.isPredictiveBackInProgress to scaffoldState.progressFraction
        }.collect { (isPredictiveBackInProgress, progressFraction) ->
            if (isPredictiveBackInProgress) {
                visualState.seekTo(
                    fraction = progressFraction,
                    targetState = SettingsSinglePanePage.List,
                )
            }
        }
    }

    LaunchedEffect(currentPage, scaffoldState.isPredictiveBackInProgress) {
        if (!scaffoldState.isPredictiveBackInProgress) {
            visualState.animateTo(currentPage)
        }
    }

    visualTransition.AnimatedContent(
        transitionSpec = {
            val layer = resolveSettingsSinglePaneTargetLayer(initialState, targetState)
            when {
                initialState == targetState -> ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                    targetContentZIndex = layer.zIndex,
                    sizeTransform = null,
                )

                targetState == SettingsSinglePanePage.Detail -> ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = motionScheme.defaultSpatialSpec(),
                    ),
                    initialContentExit = slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = motionScheme.defaultSpatialSpec(),
                    ),
                    targetContentZIndex = layer.zIndex,
                    sizeTransform = null,
                )

                else -> ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = motionScheme.defaultSpatialSpec(),
                    ),
                    initialContentExit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = motionScheme.defaultSpatialSpec(),
                    ),
                    targetContentZIndex = layer.zIndex,
                    sizeTransform = null,
                )
            }
        },
    ) { page ->
        when (page) {
            SettingsSinglePanePage.List -> listContent()
            SettingsSinglePanePage.Detail -> detailContent()
        }
    }
}

/**
 * Keeps only the outgoing detail's render key while a single-pane pop animation runs.
 * Navigation and selection continue to come exclusively from the adaptive navigator.
 */
internal fun resolveSettingsDetailRoute(
    isSinglePane: Boolean,
    navigatorRoute: SettingsRoute?,
    retainedDetailRoute: SettingsRoute?,
): SettingsRoute? = if (isSinglePane) {
    navigatorRoute ?: retainedDetailRoute
} else {
    navigatorRoute
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
    onOpenTrash: () -> Unit,
    onOpenDatabaseRecovery: () -> Unit,
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
                settingsViewModel = settingsViewModel,
                onBack = onBack
            )
        }

        SettingsRoute.Interaction,
        SettingsRoute.Autofill,
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
                onOpenTrash = onOpenTrash,
                onOpenDatabaseRecovery = onOpenDatabaseRecovery,
                onBack = onBack
            )
        }
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

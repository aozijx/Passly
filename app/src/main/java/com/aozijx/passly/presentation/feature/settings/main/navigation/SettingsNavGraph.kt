package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.ui.settings.main.SettingsMainPage
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.DatabaseRecoveryViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.main.SettingsDetailPlaceholder
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenDialogsHost
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.main.buildSettingsDialogsState
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
    val recoveryViewModel: DatabaseRecoveryViewModel = hiltViewModel()
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
                    selectedRouteKey = selectedRouteKey
                )
            }
        },
        detailPane = {
            AnimatedPane(
                // 推入和返回期间，详情始终覆盖列表，避免 Scaffold 提前切换目标层级后
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
                // 详情内容路由切换：利用 AnimatedContent 的可中断/重定向机制——
                // 动画播放中若目标路由再次变化，动画立即重定向到新目标，配合 spring
                // 弹簧动画平滑衔接，快速连续点击多个设置项也不会跳变或卡顿。
                AnimatedContent(
                    targetState = renderedDetailRoute,
                    transitionSpec = {
                        if (isSinglePane) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
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
                        }
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
                        recoveryViewModel = recoveryViewModel,
                        settingsState = settingsState,
                        onBack = if (isSinglePane) navigateBack else null
                    )
                }
            }
        }
    )

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
    recoveryViewModel: DatabaseRecoveryViewModel,
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
                recoveryViewModel = recoveryViewModel,
                settingsViewModel = settingsViewModel,
                settingsState = settingsState,
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

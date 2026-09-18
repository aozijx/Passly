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
import com.aozijx.passly.presentation.feature.settings.main.SettingsEffect
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.buildAppPasswordDialogEventHandler
import com.aozijx.passly.presentation.feature.settings.main.buildAppPasswordDialogsModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordAction
import com.aozijx.passly.presentation.feature.settings.security.validateAndSendAppPasswordAction
import com.aozijx.passly.presentation.ui.settings.main.SettingsMainPage
import com.aozijx.passly.presentation.ui.settings.main.AppPasswordDialogs
import com.aozijx.passly.presentation.ui.settings.main.rememberAppPasswordDialogStateHolder
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
fun SettingsRoute(
    settingsViewModel: SettingsViewModel,
    onOpenTrash: () -> Unit,
    onOuterBack: () -> Unit
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsDestination>()
    val scope = rememberCoroutineScope()
    val appPasswordDialogs = rememberAppPasswordDialogStateHolder()
    val context = LocalContext.current
    val mainListState = rememberLazyGridState()

    val backBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
    val isSinglePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val selectedRoute = navigator.currentDestination?.contentKey
    var retainedDetailRoute by remember { mutableStateOf<SettingsDestination?>(null) }
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
            currentPassword = appPasswordDialogs.appPasswordCurrent,
            newPassword = appPasswordDialogs.appPasswordNew,
            confirmPassword = appPasswordDialogs.appPasswordConfirm,
            settingsViewModel = settingsViewModel
        )
    }

    LaunchedEffect(Unit) {
        settingsViewModel.effects.collect { effect ->
            // 副作用
            when (effect) {
                is SettingsEffect.AppPasswordSet -> appPasswordDialogs.onAppPasswordSuccess()

                is SettingsEffect.AppPasswordChanged -> appPasswordDialogs.onAppPasswordSuccess()

                is SettingsEffect.AppPasswordDisabled -> appPasswordDialogs.onAppPasswordSuccess()

                is SettingsEffect.AppPasswordEntryAuthorized -> {
                    if (effect.alreadyEnabled) {
                        appPasswordDialogs.openAppPasswordActionDialog()
                    } else {
                        appPasswordDialogs.openSetAppPasswordDialog()
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
                        val route = SettingsDestination.fromRouteKey(routeKey) ?: return@SettingsMainPage
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
                    settingsViewModel = settingsViewModel,
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
                            val route = SettingsDestination.fromRouteKey(routeKey)
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
                            settingsViewModel = settingsViewModel,
                            onOpenTrash = onOpenTrash,
                            onBack = null,
                        )
                    }
                }
            },
        )
    }

    AppPasswordDialogs(
        state = buildAppPasswordDialogsModel(appPasswordDialogs),
        onEvent = buildAppPasswordDialogEventHandler(
            stateHolder = appPasswordDialogs,
            submitAppPasswordAction = ::submitAppPasswordAction,
        ),
    )
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

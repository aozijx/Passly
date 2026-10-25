package com.aozijx.passly.feature.settings.presentation.main.navigation

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
import androidx.compose.ui.unit.IntOffset
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsMainPage
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
    onOpenTrash: () -> Unit,
    onOuterBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsDestination>()
    val scope = rememberCoroutineScope()
    val mainListState = rememberLazyGridState()

    val backBehavior = settingsBackNavigationBehavior
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
    val navigateToDetail: (SettingsDestination) -> Unit = { route ->
        if (navigator.currentDestination?.contentKey != route) {
            scope.launch {
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = route,
                )
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
                        navigateToDetail(route)
                    },
                    selectedRouteKey = null,
                    listState = mainListState,
                )
            },
            detailContent = {
                AnimatedSettingsDetailRoute(
                    route = renderedDetailRoute,
                    onOpenTrash = onOpenTrash,
                    onNavigateTo = navigateToDetail,
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
                            navigateToDetail(route)
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
                    AnimatedSettingsDetailRoute(
                        route = renderedDetailRoute,
                        onOpenTrash = onOpenTrash,
                        onNavigateTo = navigateToDetail,
                        onBack = null,
                    )
                }
            },
        )
    }

}

/** Pops one destination so nested details return to their owning settings page. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val settingsBackNavigationBehavior = BackNavigationBehavior.PopLatest

internal enum class SettingsDetailTransitionDirection {
    Forward,
    Backward,
}

internal fun resolveSettingsDetailTransitionDirection(
    initial: SettingsDestination?,
    target: SettingsDestination?,
): SettingsDetailTransitionDirection = when {
    settingsParentDestination(target) == initial -> SettingsDetailTransitionDirection.Forward
    settingsParentDestination(initial) == target -> SettingsDetailTransitionDirection.Backward
    else -> SettingsDetailTransitionDirection.Forward
}

private fun settingsParentDestination(
    destination: SettingsDestination?,
): SettingsDestination? = when (destination) {
    SettingsDestination.Diagnostics -> SettingsDestination.DataManagement
    SettingsDestination.BackupRestore -> SettingsDestination.Permissions
    else -> null
}

/** Shared detail animation for both compact and expanded settings layouts. */
@Composable
private fun AnimatedSettingsDetailRoute(
    route: SettingsDestination?,
    onOpenTrash: () -> Unit,
    onNavigateTo: (SettingsDestination) -> Unit,
    onBack: (() -> Unit)?,
) {
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val direction = resolveSettingsDetailTransitionDirection(initialState, targetState)
            val enterOffset: (Int) -> Int = when (direction) {
                SettingsDetailTransitionDirection.Forward -> { width -> width / 5 }
                SettingsDetailTransitionDirection.Backward -> { width -> -width / 5 }
            }
            val exitOffset: (Int) -> Int = when (direction) {
                SettingsDetailTransitionDirection.Forward -> { width -> -width / 5 }
                SettingsDetailTransitionDirection.Backward -> { width -> width / 5 }
            }
            val enter = fadeIn(animationSpec = routeFadeIn) + slideInHorizontally(
                initialOffsetX = enterOffset,
                animationSpec = routeSlide,
            )
            val exit = fadeOut(animationSpec = routeFadeOut) + slideOutHorizontally(
                targetOffsetX = exitOffset,
                animationSpec = routeSlide,
            )
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "settingsDetail",
    ) { destination ->
        SettingsDetailRoute(
            route = destination,
            onOpenTrash = onOpenTrash,
            onNavigateTo = onNavigateTo,
            onBack = onBack,
        )
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

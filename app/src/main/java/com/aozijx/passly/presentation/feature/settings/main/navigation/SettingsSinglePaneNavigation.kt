package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldPredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

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
internal fun SettingsSinglePane(
    navigator: ThreePaneScaffoldNavigator<SettingsDestination>,
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

/** Keeps the outgoing render key only while a single-pane pop animation runs. */
internal fun resolveSettingsDetailRoute(
    isSinglePane: Boolean,
    navigatorRoute: SettingsDestination?,
    retainedDetailRoute: SettingsDestination?,
): SettingsDestination? = if (isSinglePane) navigatorRoute ?: retainedDetailRoute else navigatorRoute

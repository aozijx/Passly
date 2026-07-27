package com.aozijx.passly.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

/**
 * Passly 导航动画配置 - 优化版
 * 减少位移感，增加缩放和淡入淡出，消除阴影视觉差
 */
object PasslyNavigationAnim {
    private const val DURATION = 350
    private val easing = FastOutSlowInEasing

    val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        fadeIn(animationSpec = tween(DURATION, easing = easing)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(DURATION, easing = easing))
    }

    val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        fadeOut(animationSpec = tween(DURATION, easing = easing)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(DURATION, easing = easing))
    }

    val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        fadeIn(animationSpec = tween(DURATION, easing = easing)) +
                scaleIn(initialScale = 1.08f, animationSpec = tween(DURATION, easing = easing))
    }

    val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        fadeOut(animationSpec = tween(DURATION, easing = easing)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(DURATION, easing = easing))
    }
}
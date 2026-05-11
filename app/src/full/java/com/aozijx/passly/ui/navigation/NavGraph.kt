package com.aozijx.passly.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.ui.screens.detail.DetailScreen
import com.aozijx.passly.ui.screens.detail.components.AppSdkClassifier
import com.aozijx.passly.ui.screens.home.HomeScreen
import com.aozijx.passly.ui.screens.profile.ProfileScreen
import com.aozijx.passly.ui.screens.scanner.ScannerScreen
import com.aozijx.passly.ui.screens.settings.SettingsScreen

private const val ANIM_DURATION = 350
private val animEasing = FastOutSlowInEasing

/**
 * 优化后的水平进入动画：减少初始位移，增加柔和度
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalEnter(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { (it * 0.15f).toInt() },
        animationSpec = tween(ANIM_DURATION, easing = animEasing)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))
}

/**
 * 优化后的水平退出动画
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalExit(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -(it * 0.15f).toInt() },
        animationSpec = tween(ANIM_DURATION, easing = animEasing)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))
}

/**
 * 优化后的返回进入动画
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalPopEnter(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -(it * 0.15f).toInt() },
        animationSpec = tween(ANIM_DURATION, easing = animEasing)
    ) + fadeIn(animationSpec = tween(ANIM_DURATION))
}

/**
 * 优化后的返回退出动画
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.horizontalPopExit(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { (it * 0.15f).toInt() },
        animationSpec = tween(ANIM_DURATION, easing = animEasing)
    ) + fadeOut(animationSpec = tween(ANIM_DURATION))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    val bottomNavItems = Screen.bottomNavItems
    val topBarState = remember { TopBarState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    CompositionLocalProvider(LocalTopBarState provides topBarState) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (topBarState.isVisible) {
                    val navigationIcon: @Composable () -> Unit = {
                        val currentScreen = Screen.fromRoute(currentDestination?.route)
                        val shouldShowBackIcon = currentScreen?.showBackIcon == true &&
                                navController.previousBackStackEntry != null

                        if (topBarState.navigationIcon != null) {
                            topBarState.navigationIcon?.invoke()
                        } else if (shouldShowBackIcon) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                        }
                    }

                    if (topBarState.centerTitle) {
                        CenterAlignedTopAppBar(
                            title = { Text(topBarState.title) },
                            navigationIcon = navigationIcon,
                            actions = topBarState.actions,
                            scrollBehavior = scrollBehavior
                        )
                    } else {
                        TopAppBar(
                            title = { Text(topBarState.title) },
                            navigationIcon = navigationIcon,
                            actions = topBarState.actions,
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            },
            bottomBar = {
                val currentScreen = Screen.fromRoute(currentDestination?.route)
                if (currentScreen?.isBottomNav == true) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { screen.icon?.let { Icon(it, screen.title) } },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = { fadeIn(tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(tween(ANIM_DURATION)) },
                popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
                popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(navController)
                }
                composable(Screen.Vault.route) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        if (Screen.isVaultAvailable()) {
                            val intent = Intent().apply {
                                setClassName(context.packageName, BuildConfig.VAULT_ACTIVITY_CLASS)
                            }
                            context.startActivity(intent)
                        }
                        navController.popBackStack()
                    }
                }
                composable(Screen.Detail.route) {
                    DetailScreen(navController)
                }
                composable(
                    Screen.Scanner.route,
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(ANIM_DURATION, easing = animEasing)
                        ) + fadeIn()
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(ANIM_DURATION, easing = animEasing)
                        ) + fadeOut()
                    }
                ) {
                    ScannerScreen()
                }
                composable(
                    Screen.Setting.route,
                    enterTransition = { horizontalEnter() },
                    exitTransition = { horizontalExit() },
                    popEnterTransition = { horizontalPopEnter() },
                    popExitTransition = { horizontalPopExit() }
                ) {
                    SettingsScreen()
                }
                composable(
                    Screen.AppAnalysis.route,
                    enterTransition = { horizontalEnter() },
                    exitTransition = { horizontalExit() },
                    popEnterTransition = { horizontalPopEnter() },
                    popExitTransition = { horizontalPopExit() }
                ) {
                    AppSdkClassifier()
                }
            }
        }
    }
}
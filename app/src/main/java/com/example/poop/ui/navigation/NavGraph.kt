package com.example.poop.ui.navigation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.poop.ui.screens.animation.AnimationScreen
import com.example.poop.ui.screens.detail.DetailScreen
import com.example.poop.ui.screens.detail.component.AppSdkClassifier
import com.example.poop.ui.screens.home.HomeScreen
import com.example.poop.ui.screens.profile.ProfileScreen
import com.example.poop.ui.screens.scanner.ScannerScreen
import com.example.poop.ui.screens.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Home, Screen.Profile, Screen.Animation, Screen.Detail)
    
    val topBarState = remember { TopBarState() }

    CompositionLocalProvider(LocalTopBarState provides topBarState) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            topBar = {
                if (topBarState.isVisible) {
                    val navigationIcon: @Composable () -> Unit = {
                        val noBackIconRoutes = listOf(
                            Screen.Home.route,
                            Screen.Profile.route,
                            Screen.Animation.route,
                            Screen.Detail.route
                        )
                        val shouldShowBackIcon = navController.previousBackStackEntry != null && 
                                               currentDestination?.route !in noBackIconRoutes

                        if (topBarState.navigationIcon != null) {
                            topBarState.navigationIcon?.invoke()
                        } else if (shouldShowBackIcon) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    }

                    if (topBarState.centerTitle) {
                        CenterAlignedTopAppBar(
                            title = { Text(topBarState.title) },
                            navigationIcon = navigationIcon,
                            actions = topBarState.actions
                        )
                    } else {
                        TopAppBar(
                            title = { Text(topBarState.title) },
                            navigationIcon = navigationIcon,
                            actions = topBarState.actions
                        )
                    }
                }
            },
            bottomBar = {
                val shouldShowBottomBar = bottomNavItems.any { it.route == currentDestination?.route }
                if (shouldShowBottomBar) {
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
                    .padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(navController)
                }
                composable(Screen.Animation.route) {
                    AnimationScreen()
                }
                composable(Screen.Detail.route) {
                    DetailScreen(navController)
                }
                composable(
                    Screen.Scanner.route,
                    enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn() },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut() }
                ) {
                    ScannerScreen()
                }
                composable(
                    Screen.Setting.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut() }
                ) {
                    SettingsScreen()
                }
                composable(
                    Screen.AppAnalysis.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut() }
                ) {
                    AppSdkClassifier()
                }
            }
        }
    }
}

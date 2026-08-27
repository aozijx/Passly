package com.aozijx.passly.presentation.feature.shell.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import com.aozijx.passly.presentation.feature.shell.AppShellUiAction
import com.aozijx.passly.presentation.feature.shell.AppShellViewModel

internal typealias FeatureGraphRegistration = NavGraphBuilder.(
    context: ShellNavigationContext,
    sharedTransitionScope: SharedTransitionScope,
    isDatabaseInitializing: Boolean,
) -> Unit

/** Creates the shell NavHost and delegates all feature destination registration. */
@Composable
internal fun PasslyNavHost(
    navController: NavHostController,
    appShellViewModel: AppShellViewModel,
    isDatabaseInitializing: Boolean = false,
    registerFeatureGraphs: FeatureGraphRegistration,
) {
    val authContinuation = remember { ShellAuthContinuation() }

    LaunchedEffect(appShellViewModel) {
        appShellViewModel.authResults.collect(authContinuation::onResult)
    }

    val navigationContext = ShellNavigationContext(
        navigateBack = { navController.popBackStack() },
        navigateToRoute = navController::navigate,
        navigateToSingleTopRoute = { route ->
            navController.navigate(route) { launchSingleTop = true }
        },
        requestAuthentication = { onSuccess ->
            authContinuation.replace(onSuccess)
            appShellViewModel.onAction(AppShellUiAction.RequestAuth)
        },
        requestReauthentication = { onSuccess ->
            authContinuation.replace(onSuccess)
            appShellViewModel.onAction(AppShellUiAction.RequestReauth)
        },
        requestSensitiveAccess = { action, accessLevel, onSuccess ->
            authContinuation.replace(onSuccess)
            appShellViewModel.onAction(
                AppShellUiAction.RequestSensitiveAccess(action, accessLevel),
            )
        },
        onUserInteraction = {
            appShellViewModel.onAction(AppShellUiAction.UpdateInteraction)
        },
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        SharedTransitionLayout {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = AppRoute.Vault.route,
                enterTransition = PasslyNavigationAnim.enterTransition,
                exitTransition = PasslyNavigationAnim.exitTransition,
                popEnterTransition = PasslyNavigationAnim.popEnterTransition,
                popExitTransition = PasslyNavigationAnim.popExitTransition,
            ) {
                registerFeatureGraphs(
                    navigationContext,
                    this@SharedTransitionLayout,
                    isDatabaseInitializing,
                )
            }
        }
    }
}

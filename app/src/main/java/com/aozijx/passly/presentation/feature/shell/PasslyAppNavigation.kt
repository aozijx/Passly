package com.aozijx.passly.presentation.feature.shell

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.core.ui.adaptive.ProvidePasslyAdaptiveLayout
import com.aozijx.passly.presentation.feature.settings.main.navigation.registerSettingsGraph
import com.aozijx.passly.presentation.feature.shell.navigation.PasslyNavHost
import com.aozijx.passly.presentation.feature.vault.navigation.registerVaultGraph

@Composable
internal fun PasslyAppNavigation(
    onUserInteraction: () -> Unit,
) {
    val navController = rememberNavController()

    ProvidePasslyAdaptiveLayout {
        PasslyNavHost(
            navController = navController,
            onUserInteraction = onUserInteraction,
        ) { context, sharedTransitionScope ->
            registerVaultGraph(
                context = context,
                navController = navController,
                sharedTransitionScope = sharedTransitionScope,
            )
            registerSettingsGraph(context)
        }
    }
}

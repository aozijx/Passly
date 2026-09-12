package com.aozijx.passly.presentation.feature.shell

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.presentation.feature.shell.navigation.PasslyNavHost
import com.aozijx.passly.presentation.feature.shell.AppShellViewModel
import com.aozijx.passly.core.ui.adaptive.ProvidePasslyAdaptiveLayout
import com.aozijx.passly.presentation.feature.vault.list.VaultViewModel
import com.aozijx.passly.presentation.feature.vault.navigation.registerVaultGraph
import com.aozijx.passly.presentation.feature.settings.main.navigation.registerSettingsGraph

@Composable
internal fun AppShellContent(
    appShellViewModel: AppShellViewModel
) {
    val vaultViewModel: VaultViewModel = hiltViewModel()
    val navController = rememberNavController()

    ProvidePasslyAdaptiveLayout {
        PasslyNavHost(
            navController = navController,
            appShellViewModel = appShellViewModel,
        ) { context, sharedTransitionScope ->
            registerVaultGraph(
                context = context,
                vaultViewModel = vaultViewModel,
                sharedTransitionScope = sharedTransitionScope,
            )
            registerSettingsGraph(context)
        }
    }
}

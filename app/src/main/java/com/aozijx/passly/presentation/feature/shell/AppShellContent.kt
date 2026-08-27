package com.aozijx.passly.presentation.feature.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val mainUiState by appShellViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    ProvidePasslyAdaptiveLayout {
        PasslyNavHost(
            navController = navController,
            appShellViewModel = appShellViewModel,
            isDatabaseInitializing = mainUiState.isDatabaseInitializing
        ) { context, sharedTransitionScope, isDatabaseInitializing ->
            registerVaultGraph(
                context = context,
                vaultViewModel = vaultViewModel,
                sharedTransitionScope = sharedTransitionScope,
                isDatabaseInitializing = isDatabaseInitializing,
            )
            registerSettingsGraph(context)
        }
    }
}

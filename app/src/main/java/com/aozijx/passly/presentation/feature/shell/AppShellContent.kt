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
            vaultViewModel = vaultViewModel,
            isDatabaseInitializing = mainUiState.isDatabaseInitializing
        )
    }
}

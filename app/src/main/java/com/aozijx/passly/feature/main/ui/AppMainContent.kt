package com.aozijx.passly.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.app.navigation.PasslyNavHost
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.vault.VaultViewModel

@Composable
internal fun AppMainContent(
    mainViewModel: MainViewModel
) {
    val vaultViewModel: VaultViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    PasslyNavHost(
        navController = navController,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
        isDatabaseInitializing = mainUiState.isDatabaseInitializing
    )
}

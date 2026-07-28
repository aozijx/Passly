package com.aozijx.passly.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.DetailViewModel
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.page.DetailScreen
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.settings.SettingsScreen
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.vault.VaultContent
import com.aozijx.passly.feature.vault.VaultViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Passly 应用导航宿主
 */
@Composable
fun PasslyNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    isDatabaseInitializing: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = AppRoute.Vault.route,
            enterTransition = PasslyNavigationAnim.enterTransition,
            exitTransition = PasslyNavigationAnim.exitTransition,
            popEnterTransition = PasslyNavigationAnim.popEnterTransition,
            popExitTransition = PasslyNavigationAnim.popExitTransition
        ) {
        composable(AppRoute.Vault.route) {
            VaultContent(
                mainViewModel = mainViewModel,
                vaultViewModel = vaultViewModel,
                onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onShowDetail = { entry ->
                    navController.navigate(AppRoute.Detail.createRoute(entry.id))
                },
                isDatabaseInitializing = isDatabaseInitializing
            )
        }

        composable(
            route = AppRoute.Detail.route,
            arguments = listOf(
                navArgument(AppRoute.Detail.ARG_ENTRY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getString(AppRoute.Detail.ARG_ENTRY_ID)
                ?: return@composable

            val detailViewModel: DetailViewModel = hiltViewModel()
            val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

            val totpState by vaultViewModel.totpStatesFlow.collectAsStateWithLifecycle()
            val currentOtpState = totpState[entryId]

            LaunchedEffect(detailViewModel) {
                detailViewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is DetailEffect.EntryUpdated -> vaultViewModel.updateVaultEntry(effect.entry)
                        DetailEffect.IconPickerRequested -> vaultViewModel.showDetailIconPicker()
                    }
                }
            }

            var initialEntry by remember { mutableStateOf<VaultEntry?>(null) }
            DisposableEffect(entryId, detailViewModel, vaultViewModel) {
                val loadJob = vaultViewModel.loadEntryById(entryId) { initialEntry = it }
                onDispose {
                    loadJob.cancel()
                    initialEntry = null
                    detailViewModel.handleIntent(DetailIntent.ClearSensitiveState)
                }
            }

            initialEntry?.let { entry ->
                DetailScreen(
                    initialEntry = entry,
                    uiState = detailUiState,
                    otpUiState = currentOtpState,
                    onBack = { navController.popBackStack() },
                    onEvent = detailViewModel::handleIntent,
                    onUpdateInteraction = { mainViewModel.handleIntent(MainIntent.UpdateInteraction) },
                    onUpdateVaultEntry = { vaultViewModel.updateVaultEntry(it) },
                    onShowIconPicker = { vaultViewModel.showDetailIconPicker() },
                    onAutoUnlockTotp = { vaultViewModel.autoUnlockTotp(it.id) },
                    onAuthenticate = { success ->
                        mainViewModel.requestAuth(onSuccess = success)
                    }
                )
            }
        }

        composable(AppRoute.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                onBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel
            )
        }
        }
    }
}

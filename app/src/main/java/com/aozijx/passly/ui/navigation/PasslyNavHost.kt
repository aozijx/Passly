package com.aozijx.passly.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.detail.DetailViewModel
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.page.DetailScreen
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.settings.SettingsScreen
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.datamanagement.DataViewModel
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
    backupViewModel: BackupViewModel,
    onPlainExportClick: () -> Unit,
    isDatabaseInitializing: Boolean = false
) {
    val dataViewModel: DataViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()
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
                backupViewModel = backupViewModel,
                backupDirectoryUri = dataState.directoryUri,
                onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onPlainExportClick = onPlainExportClick,
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
            val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(detailViewModel) {
                detailViewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is DetailEffect.EntryUpdated -> vaultViewModel.updateVaultEntry(effect.entry)
                        DetailEffect.IconPickerRequested -> vaultViewModel.showDetailIconPicker()
                    }
                }
            }

            var initialEntry by remember { mutableStateOf<VaultEntry?>(null) }
            LaunchedEffect(entryId) {
                vaultViewModel.loadEntryById(entryId) { initialEntry = it }
            }

            initialEntry?.let { entry ->
                DetailScreen(
                    initialEntry = entry,
                    uiState = detailUiState,
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
            val onUpdateInteraction: () -> Unit =
                { mainViewModel.handleIntent(MainIntent.UpdateInteraction) }
            SettingsScreen(
                onBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel,
                onUpdateInteraction = onUpdateInteraction
            )
        }
        }
    }
}

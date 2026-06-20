package com.aozijx.passly.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.ui.features.backup.BackupCoordinator
import com.aozijx.passly.ui.features.detail.DetailViewModel
import com.aozijx.passly.ui.features.detail.contract.DetailEffect
import com.aozijx.passly.ui.features.detail.page.DetailScreen
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.main.contract.MainIntent
import com.aozijx.passly.ui.features.settings.SettingsScreen
import com.aozijx.passly.ui.features.settings.SettingsViewModel
import com.aozijx.passly.ui.features.settings.data.DataViewModel
import com.aozijx.passly.ui.features.vault.VaultContent
import com.aozijx.passly.ui.features.vault.VaultViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Passly 应用导航宿主
 */
@Composable
fun PasslyNavHost(
    navController: NavHostController,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    backupCoordinator: BackupCoordinator,
    onPlainExportClick: () -> Unit
) {
    val dataViewModel: DataViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()

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
                activity = activity,
                mainViewModel = mainViewModel,
                vaultViewModel = vaultViewModel,
                backupCoordinator = backupCoordinator,
                backupDirectoryUri = dataState.directoryUri,
                onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onPlainExportClick = onPlainExportClick,
                onShowDetail = { entry ->
                    navController.navigate(AppRoute.Detail.createRoute(entry.id))
                }
            )
        }

        composable(
            route = AppRoute.Detail.route,
            arguments = listOf(
                navArgument(AppRoute.Detail.ARG_ENTRY_ID) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getInt(AppRoute.Detail.ARG_ENTRY_ID)
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
                    totpStates = vaultUiState.totpStates,
                    onBack = { navController.popBackStack() },
                    onEvent = detailViewModel::onEvent,
                    onUpdateInteraction = { mainViewModel.handleIntent(MainIntent.UpdateInteraction) },
                    onUpdateVaultEntry = { vaultViewModel.updateVaultEntry(it) },
                    onShowIconPicker = { vaultViewModel.showDetailIconPicker() },
                    onAutoUnlockTotp = { vaultViewModel.autoUnlockTotp(it) },
                    onAuthenticate = { act, title, subtitle, success ->
                        mainViewModel.requestAuth(act, title, subtitle, onSuccess = success)
                    },
                    activity = activity
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
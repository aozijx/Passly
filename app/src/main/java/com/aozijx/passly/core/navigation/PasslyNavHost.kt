package com.aozijx.passly.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.detail.DetailViewModel
import com.aozijx.passly.features.detail.contract.DetailEffect
import com.aozijx.passly.features.detail.page.DetailScreen
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.settings.SettingsScreen
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultContent
import com.aozijx.passly.features.vault.VaultViewModel
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
    onPlainExportClick: () -> Unit
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
                activity = activity,
                mainViewModel = mainViewModel,
                vaultViewModel = vaultViewModel,
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

            val application = LocalContext.current.applicationContext as android.app.Application
            val detailViewModel: DetailViewModel = viewModel(
                factory = appViewModelFactory(application)
            )
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
            val application = LocalContext.current.applicationContext as android.app.Application
            val configProvider: UserConfigProvider = viewModel(
                factory = appViewModelFactory(application)
            )
            val authViewModel: SettingsViewModel = viewModel(
                factory = appViewModelFactory(application)
            )
            val onUpdateInteraction: () -> Unit =
                { mainViewModel.handleIntent(MainIntent.UpdateInteraction) }
            SettingsScreen(
                onBack = { navController.popBackStack() },
                configProvider = configProvider,
                authViewModel = authViewModel,
                onUpdateInteraction = onUpdateInteraction
            )
        }
    }
}
package com.aozijx.passly.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.domain.model.core.VaultEntry
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
 *
 * 以 [AppRoute.Vault] 为起始目的地，管理以下页面跳转：
 *  - Vault → Detail（点击列表条目）
 *  - Vault → Settings（点击设置入口）
 *  - Detail / Settings → Vault（返回）
 */
@Composable
fun PasslyNavHost(
    navController: NavHostController,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    settingsViewModel: SettingsViewModel,
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
                settingsViewModel = settingsViewModel,
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

            // 获取详情页 ViewModel 并采集状态
            val detailViewModel: DetailViewModel = viewModel()
            val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()
            val totpStates by vaultViewModel.totpStates.collectAsState()

            // 监听 DetailViewModel 的 Side Effects
            LaunchedEffect(detailViewModel) {
                detailViewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is DetailEffect.EntryUpdated -> vaultViewModel.updateVaultEntry(effect.entry)
                        DetailEffect.IconPickerRequested -> vaultViewModel.showDetailIconPicker()
                    }
                }
            }

            // 获取初始 Entry 数据用于过渡
            var initialEntry by remember { mutableStateOf<VaultEntry?>(null) }
            LaunchedEffect(entryId) {
                vaultViewModel.loadEntryById(entryId) { initialEntry = it }
            }

            initialEntry?.let { entry ->
                DetailScreen(
                    initialEntry = entry,
                    uiState = detailUiState,
                    totpStates = totpStates,
                    onBack = { navController.popBackStack() },
                    onEvent = detailViewModel::onEvent,
                    onUpdateInteraction = { mainViewModel.handleIntent(MainIntent.UpdateInteraction) },
                    onUpdateVaultEntry = { vaultViewModel.updateVaultEntry(it) },
                    onShowIconPicker = { vaultViewModel.showDetailIconPicker() },
                    onAutoUnlockTotp = { vaultViewModel.autoUnlockTotp(it) },
                    onAuthenticate = { act, title, subtitle, success ->
                        mainViewModel.authenticate(act, title, subtitle, onSuccess = success)
                    },
                    activity = activity
                )
            }
        }

        composable(AppRoute.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
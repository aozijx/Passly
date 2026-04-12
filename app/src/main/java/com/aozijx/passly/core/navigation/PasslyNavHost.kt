package com.aozijx.passly.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.page.DetailScreen
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.settings.SettingsScreen
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultContent
import com.aozijx.passly.features.vault.VaultViewModel

/**
 * Passly 应用导航宿主
 *
 * 以 [AppRoute.Vault] 为起始目的地，管理以下页面跳转：
 *  - Vault → Detail（点击列表条目）
 *  - Vault → Settings（点击设置入口）
 *  - Detail / Settings → Vault（返回）
 *
 * 认证状态（已登录 / 已锁定）由调用方根据 MainViewModel.uiState
 * 决定是否渲染本 NavHost，无需在内部处理。
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

            var entry by remember { mutableStateOf<VaultEntry?>(null) }
            LaunchedEffect(entryId) {
                vaultViewModel.loadEntryById(entryId) { entry = it }
            }

            entry?.let { currentEntry ->
                DetailScreen(
                    initialEntry = currentEntry,
                    onBack = { navController.popBackStack() },
                    activity = activity,
                    mainViewModel = mainViewModel,
                    vaultViewModel = vaultViewModel
                )
            }
        }

        composable(AppRoute.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
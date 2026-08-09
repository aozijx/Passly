package com.aozijx.passly.app.navigation

import androidx.compose.animation.SharedTransitionLayout
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
import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.domain.authentication.SensitiveAccessLevel
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.DetailViewModel
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.page.DetailScreen
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.scanner.VaultScanner
import com.aozijx.passly.feature.settings.SettingsScreen
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.vault.VaultContent
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.contract.VaultIntent
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardScreen
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardViewModel
import com.aozijx.passly.feature.vault.editor.otp.AddOtpScreen
import com.aozijx.passly.feature.vault.editor.otp.AddOtpViewModel
import com.aozijx.passly.feature.vault.editor.password.AddPasswordScreen
import com.aozijx.passly.feature.vault.editor.password.AddPasswordViewModel
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
        SharedTransitionLayout {
            val sharedTransitionScope = this
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
            val animatedVisibilityScope = this
            VaultContent(
                vaultViewModel = vaultViewModel,
                requestAuthentication = { onSuccess ->
                    mainViewModel.requestAuth(onSuccess = onSuccess)
                },
                requestReauthentication = { onSuccess ->
                    mainViewModel.requestReauth(onSuccess = onSuccess)
                },
                requestSensitiveCopy = { onSuccess ->
                    mainViewModel.requestSensitiveAccess(
                        action = SensitiveAccessAction.COPY,
                        accessLevel = SensitiveAccessLevel.STANDARD,
                        onSuccess = onSuccess
                    )
                },
                onUserInteraction = {
                    mainViewModel.handleIntent(MainIntent.UpdateInteraction)
                },
                onAddPassword = {
                    navController.navigate(AppRoute.AddPassword.route) {
                        launchSingleTop = true
                    }
                },
                onAddOtp = {
                    navController.navigate(AppRoute.AddOtp.route) {
                        launchSingleTop = true
                    }
                },
                onAddBankCard = {
                    navController.navigate(AppRoute.AddBankCard.route) {
                        launchSingleTop = true
                    }
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onShowDetail = { entry ->
                    navController.navigate(AppRoute.Detail.createRoute(entry.id))
                },
                isDatabaseInitializing = isDatabaseInitializing
            )
        }

            composable(AppRoute.AddPassword.route) {
                val animatedVisibilityScope = this
                val addPasswordViewModel: AddPasswordViewModel = hiltViewModel()
                AddPasswordScreen(
                    viewModel = addPasswordViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onUserInteraction = {
                        mainViewModel.handleIntent(MainIntent.UpdateInteraction)
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

                composable(AppRoute.AddOtp.route) {
                    val animatedVisibilityScope = this
                    val addOtpViewModel: AddOtpViewModel = hiltViewModel()
                    AddOtpScreen(
                        viewModel = addOtpViewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onUserInteraction = {
                            mainViewModel.handleIntent(MainIntent.UpdateInteraction)
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        scannerContent = { onResult, onDismiss ->
                            VaultScanner(
                                onSaveOtp = onResult,
                                onDismiss = onDismiss
                            )
                        }
                )
            }

                composable(AppRoute.AddBankCard.route) {
                    val animatedVisibilityScope = this
                    val addBankCardViewModel: AddBankCardViewModel = hiltViewModel()
                    AddBankCardScreen(
                        viewModel = addBankCardViewModel,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onUserInteraction = {
                            mainViewModel.handleIntent(MainIntent.UpdateInteraction)
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
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
                        is DetailEffect.EntryUpdated -> vaultViewModel.onIntent(
                            VaultIntent.UpdateVaultEntry(
                                effect.entry
                            )
                        )
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
                    onEvent = detailViewModel::handleIntent,
                    onUpdateInteraction = { mainViewModel.handleIntent(MainIntent.UpdateInteraction) },
                    onAutoUnlockTotp = { vaultViewModel.onIntent(VaultIntent.AutoUnlockTotp(it.id)) },
                    onOpenRelatedEntry = {
                        navController.navigate(AppRoute.Detail.createRoute(it.id))
                    },
                    onAuthenticate = DetailAuthenticate { action, accessLevel, success ->
                        mainViewModel.requestSensitiveAccess(
                            action = action,
                            accessLevel = accessLevel,
                            onSuccess = success
                        )
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
}

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
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.DetailViewModel
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.page.DetailScreen
import com.aozijx.passly.app.shell.AppShellViewModel
import com.aozijx.passly.app.shell.contract.AppShellAuthResult
import com.aozijx.passly.app.shell.contract.AppShellIntent
import com.aozijx.passly.feature.scanner.VaultScanner
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.navigation.SettingsNavGraph
import com.aozijx.passly.presentation.vault.VaultContent
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.contract.VaultIntent
import com.aozijx.passly.presentation.vault.editor.bankcard.AddBankCardScreen
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardViewModel
import com.aozijx.passly.presentation.vault.editor.otp.AddOtpScreen
import com.aozijx.passly.feature.vault.editor.otp.AddOtpViewModel
import com.aozijx.passly.presentation.vault.editor.password.AddPasswordScreen
import com.aozijx.passly.feature.vault.editor.password.AddPasswordViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Passly 应用导航宿主
 */
@Composable
fun PasslyNavHost(
    navController: NavHostController,
    appShellViewModel: AppShellViewModel,
    vaultViewModel: VaultViewModel,
    isDatabaseInitializing: Boolean = false
) {
    var pendingAuthCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 认证请求可能来自任意导航目的地。监听器必须位于 NavHost 外层，
    // 否则离开保险库首页后，对应 composable 被移除，成功回调也会丢失。
    LaunchedEffect(appShellViewModel) {
        appShellViewModel.authResults.collect { result ->
            when (result) {
                AppShellAuthResult.Success -> {
                    pendingAuthCallback?.invoke()
                    pendingAuthCallback = null
                }

                AppShellAuthResult.NotAuthorized -> {
                    pendingAuthCallback = null
                }
            }
        }
    }

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
                            pendingAuthCallback = onSuccess
                            appShellViewModel.handleIntent(AppShellIntent.RequestAuth)
                        },
                        requestReauthentication = { onSuccess ->
                            pendingAuthCallback = onSuccess
                            appShellViewModel.handleIntent(AppShellIntent.RequestReauth)
                        },
                        requestSensitiveCopy = { onSuccess ->
                            pendingAuthCallback = onSuccess
                            appShellViewModel.handleIntent(
                                AppShellIntent.RequestSensitiveAccess(
                                    action = SensitiveAccessAction.COPY,
                                    accessLevel = SensitiveAccessLevel.STANDARD
                                )
                            )
                        },
                        onUserInteraction = {
                            appShellViewModel.handleIntent(AppShellIntent.UpdateInteraction)
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
                            navController.navigate(AppRoute.Detail.createRoute(entry.id.value))
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
                            appShellViewModel.handleIntent(AppShellIntent.UpdateInteraction)
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
                            appShellViewModel.handleIntent(AppShellIntent.UpdateInteraction)
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
                            appShellViewModel.handleIntent(AppShellIntent.UpdateInteraction)
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
                                    VaultIntent.UpdateEntry(
                                        effect.entry
                                    )
                                )
                            }
                        }
                    }

                    var initialEntry by remember { mutableStateOf<Entry?>(null) }
                    LaunchedEffect(entryId) {
                        initialEntry = vaultViewModel.loadEntryById(entryId)
                    }
                    DisposableEffect(entryId) {
                        onDispose {
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
                            onBack = { navController.popBackStack() },
                            onUpdateInteraction = {
                                appShellViewModel.handleIntent(AppShellIntent.UpdateInteraction)
                            },
                            onAutoUnlockTotp = {
                                vaultViewModel.onIntent(
                                    VaultIntent.AutoUnlockTotp(
                                        it.id.value
                                    )
                                )
                            },
                            onOpenRelatedEntry = {
                                navController.navigate(AppRoute.Detail.createRoute(it.id.value))
                            },
                            onAuthenticate = DetailAuthenticate { action, accessLevel, success ->
                                pendingAuthCallback = success
                                appShellViewModel.handleIntent(
                                    AppShellIntent.RequestSensitiveAccess(
                                        action = action,
                                        accessLevel = accessLevel
                                    )
                                )
                            }
                        )
                    }
                }

                composable(AppRoute.Settings.route) {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    SettingsNavGraph(
                        onOuterBack = { navController.popBackStack() },
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

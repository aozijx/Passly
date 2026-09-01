package com.aozijx.passly.presentation.feature.vault.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.scanner.navigation.VaultOtpScannerRoute
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.shell.navigation.ShellNavigationContext
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEffect
import com.aozijx.passly.presentation.feature.vault.detail.DetailHost
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailViewModel
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardEditorHost
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardViewModel
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpEditorHost
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpViewModel
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordEditorHost
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordViewModel
import com.aozijx.passly.presentation.feature.vault.list.VaultHost
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.VaultViewModel
import com.aozijx.passly.presentation.feature.vault.trash.TrashHost
import com.aozijx.passly.presentation.feature.vault.trash.TrashViewModel
import kotlinx.coroutines.flow.collectLatest

internal fun NavGraphBuilder.registerVaultGraph(
    context: ShellNavigationContext,
    vaultViewModel: VaultViewModel,
    sharedTransitionScope: SharedTransitionScope,
    isDatabaseInitializing: Boolean,
) {
    composable(AppRoute.Vault.route) {
        VaultDestinationContent(
            context = context,
            vaultViewModel = vaultViewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            isDatabaseInitializing = isDatabaseInitializing,
        )
    }

    composable(AppRoute.Trash.route) {
        val trashViewModel: TrashViewModel = hiltViewModel()
        VaultDestinationContent(
            context = context,
            vaultViewModel = vaultViewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            isDatabaseInitializing = isDatabaseInitializing,
        )
        TrashHost(viewModel = trashViewModel, onDismiss = context.navigateBack)
    }

    composable(AppRoute.AddPassword.route) {
        val viewModel: AddPasswordViewModel = hiltViewModel()
        AddPasswordEditorHost(
            viewModel = viewModel,
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            onUserInteraction = context.onUserInteraction,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(AppRoute.AddOtp.route) {
        val viewModel: AddOtpViewModel = hiltViewModel()
        AddOtpEditorHost(
            viewModel = viewModel,
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            onUserInteraction = context.onUserInteraction,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            scannerContent = { onResult, onDismiss ->
                VaultOtpScannerRoute(onResult = onResult, onDismiss = onDismiss)
            },
        )
    }

    composable(AppRoute.AddBankCard.route) {
        val viewModel: AddBankCardViewModel = hiltViewModel()
        AddBankCardEditorHost(
            viewModel = viewModel,
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            onUserInteraction = context.onUserInteraction,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(
        route = AppRoute.Detail.route,
        arguments = listOf(
            navArgument(AppRoute.Detail.ARG_ENTRY_ID) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments
            ?.getString(AppRoute.Detail.ARG_ENTRY_ID)
            ?: return@composable
        val detailViewModel: DetailViewModel = hiltViewModel()
        val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()
        val totpState by vaultViewModel.totpStatesFlow.collectAsStateWithLifecycle()
        val currentOtpState = totpState[entryId]
        var otpQrUri by remember(entryId) { mutableStateOf<String?>(null) }

        LaunchedEffect(detailViewModel) {
            detailViewModel.effects.collectLatest { effect ->
                when (effect) {
                    is DetailEffect.EntryUpdated -> vaultViewModel.onAction(
                        VaultUiAction.UpdateEntry(effect.entry),
                    )
                    is DetailEffect.ShowOtpQr -> otpQrUri = effect.uri
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
                detailViewModel.onAction(DetailUiAction.ClearSensitiveState)
            }
        }

        initialEntry?.let { entry ->
            DetailHost(
                initialEntry = entry,
                uiState = detailUiState,
                otpUiState = currentOtpState,
                otpQrUri = otpQrUri,
                onAction = detailViewModel::onAction,
                onCopySensitive = detailViewModel::copySensitive,
                onOtpQrDismiss = { otpQrUri = null },
                onBack = context.navigateBack,
                onUpdateInteraction = context.onUserInteraction,
                onAutoUnlockTotp = {
                    vaultViewModel.onAction(VaultUiAction.AutoUnlockTotp(it.id.value))
                },
                onOpenRelatedEntry = {
                    context.navigateToRoute(AppRoute.Detail.createRoute(it.id.value))
                },
                onAuthenticate = DetailAuthenticate { action, accessLevel, success ->
                    context.requestSensitiveAccess(action, accessLevel, success)
                },
            )
        }
    }
}

@Composable
private fun VaultDestinationContent(
    context: ShellNavigationContext,
    vaultViewModel: VaultViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isDatabaseInitializing: Boolean,
) {
    VaultHost(
        vaultViewModel = vaultViewModel,
        requestAuthentication = context.requestAuthentication,
        requestReauthentication = context.requestReauthentication,
        requestSensitiveCopy = { onSuccess ->
            context.requestSensitiveAccess(
                com.aozijx.passly.domain.access.model.SensitiveAccessAction.COPY,
                com.aozijx.passly.app.security.SensitiveAccessLevel.STANDARD,
                onSuccess,
            )
        },
        onUserInteraction = context.onUserInteraction,
        onAddPassword = { context.navigateToSingleTopRoute(AppRoute.AddPassword.route) },
        onAddOtp = { context.navigateToSingleTopRoute(AppRoute.AddOtp.route) },
        onAddBankCard = { context.navigateToSingleTopRoute(AppRoute.AddBankCard.route) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onSettingsClick = { context.navigateToRoute(AppRoute.Settings.route) },
        onShowDetail = { entryId -> context.navigateToRoute(AppRoute.Detail.createRoute(entryId)) },
        isDatabaseInitializing = isDatabaseInitializing,
    )
}

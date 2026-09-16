package com.aozijx.passly.presentation.feature.vault.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.aozijx.passly.presentation.feature.scanner.navigation.VaultOtpScannerRoute
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.shell.navigation.ShellNavigationContext
import com.aozijx.passly.presentation.feature.vault.detail.DetailRoute
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardEditorRoute
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardViewModel
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpEditorRoute
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpViewModel
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordEditorRoute
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordViewModel
import com.aozijx.passly.presentation.feature.vault.list.VaultRoute
import com.aozijx.passly.presentation.feature.vault.list.VaultViewModel
import com.aozijx.passly.presentation.feature.vault.trash.TrashRoute
import com.aozijx.passly.presentation.feature.vault.trash.TrashViewModel

internal fun NavGraphBuilder.registerVaultGraph(
    context: ShellNavigationContext,
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
) {
    navigation(
        startDestination = AppRoute.Vault.route,
        route = AppRoute.VaultGraph.route,
    ) {
        composable(AppRoute.Vault.route) { backStackEntry ->
            VaultDestinationContent(
                context = context,
                vaultViewModel = vaultGraphViewModel(navController, backStackEntry),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
            )
        }

        composable(AppRoute.Trash.route) { backStackEntry ->
            val trashViewModel: TrashViewModel = hiltViewModel()
            VaultDestinationContent(
                context = context,
                vaultViewModel = vaultGraphViewModel(navController, backStackEntry),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
            )
            TrashRoute(viewModel = trashViewModel, onDismiss = context.navigateBack)
        }
    }

    composable(AppRoute.AddPassword.route) {
        val viewModel: AddPasswordViewModel = hiltViewModel()
        AddPasswordEditorRoute(
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
        AddOtpEditorRoute(
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
        AddBankCardEditorRoute(
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
        DetailRoute(
            entryId = entryId,
            onBack = context.navigateBack,
            onUpdateInteraction = context.onUserInteraction,
            onOpenRelatedEntry = {
                context.navigateToRoute(AppRoute.Detail.createRoute(it.id.value))
            },
        )
    }
}

@Composable
private fun vaultGraphViewModel(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
): VaultViewModel {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoute.VaultGraph.route)
    }
    return hiltViewModel<VaultViewModel>(owner)
}

@Composable
private fun VaultDestinationContent(
    context: ShellNavigationContext,
    vaultViewModel: VaultViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    VaultRoute(
        vaultViewModel = vaultViewModel,
        onAddPassword = { context.navigateToSingleTopRoute(AppRoute.AddPassword.route) },
        onAddOtp = { context.navigateToSingleTopRoute(AppRoute.AddOtp.route) },
        onAddBankCard = { context.navigateToSingleTopRoute(AppRoute.AddBankCard.route) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onSettingsClick = { context.navigateToRoute(AppRoute.Settings.route) },
        onShowDetail = { entryId -> context.navigateToRoute(AppRoute.Detail.createRoute(entryId)) },
    )
}

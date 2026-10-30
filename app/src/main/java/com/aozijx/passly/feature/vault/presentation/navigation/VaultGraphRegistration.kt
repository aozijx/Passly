package com.aozijx.passly.feature.vault.presentation.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
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
import com.aozijx.passly.feature.shell.presentation.navigation.AppRoute
import com.aozijx.passly.feature.shell.presentation.navigation.ShellNavigationContext
import com.aozijx.passly.feature.vault.presentation.detail.DetailRoute
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.AddBankCardEditorRoute
import com.aozijx.passly.feature.vault.presentation.editor.otp.AddOtpEditorRoute
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordEditorRoute
import com.aozijx.passly.feature.vault.presentation.editor.wifi.AddWifiEditorRoute
import com.aozijx.passly.feature.vault.presentation.editor.wifi.AddWifiInitialContent
import com.aozijx.passly.feature.vault.entry.EditorSeedToken
import com.aozijx.passly.feature.vault.entry.EditorSeedTokenParseResult
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.list.VaultRoute
import com.aozijx.passly.feature.vault.presentation.list.VaultViewModel
import com.aozijx.passly.feature.vault.presentation.trash.TrashRoute

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
            val seedCoordinator = vaultGraphSeedCoordinator(navController, backStackEntry)
            VaultRoute(
                navigation = context.toVaultNavigation(seedCoordinator),
                vaultViewModel = vaultGraphViewModel(navController, backStackEntry),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
            )
        }

        composable(AppRoute.Trash.route) { backStackEntry ->
            val seedCoordinator = vaultGraphSeedCoordinator(navController, backStackEntry)
            VaultRoute(
                navigation = context.toVaultNavigation(seedCoordinator),
                vaultViewModel = vaultGraphViewModel(navController, backStackEntry),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
                isEffectsPage = false,
            )
            TrashRoute(onDismiss = context.navigateBack)
        }
    }

    composable(AppRoute.AddPassword.route) {
        AddPasswordEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(AppRoute.AddOtp.route) {
        AddOtpEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(AppRoute.AddBankCard.route) {
        AddBankCardEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(AppRoute.AddWifi.route) {
        AddWifiEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    registerEditorEditDestination(
        navGraphBuilder = this,
        route = AppRoute.EditPassword.route,
    ) { entryId ->
        AddPasswordEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            editEntryId = entryId,
        )
    }
    registerEditorEditDestination(
        navGraphBuilder = this,
        route = AppRoute.EditOtp.route,
    ) { entryId ->
        AddOtpEditorRoute(context.navigateBack, context.navigateBack, sharedTransitionScope, this, editEntryId = entryId)
    }
    registerEditorEditDestination(
        navGraphBuilder = this,
        route = AppRoute.EditBankCard.route,
    ) { entryId ->
        AddBankCardEditorRoute(context.navigateBack, context.navigateBack, sharedTransitionScope, this, editEntryId = entryId)
    }
    registerEditorEditDestination(
        navGraphBuilder = this,
        route = AppRoute.EditWifi.route,
    ) { entryId ->
        AddWifiEditorRoute(context.navigateBack, context.navigateBack, sharedTransitionScope, this, editEntryId = entryId)
    }

    composable(
        route = AppRoute.AddWifiFromSeed.route,
        arguments = listOf(
            navArgument(AppRoute.AddWifiFromSeed.ARG_SEED_TOKEN) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val seedCoordinator = vaultGraphSeedCoordinator(navController, backStackEntry)
        val loadInitialContent = {
            when (
                val parsed = EditorSeedToken.parse(
                    backStackEntry.arguments
                        ?.getString(AppRoute.AddWifiFromSeed.ARG_SEED_TOKEN)
                        .orEmpty(),
                )
            ) {
                is EditorSeedTokenParseResult.Valid -> when (
                    val consumed = seedCoordinator.consume(parsed.token)
                ) {
                    is EntryEditorSeedConsumeResult.Consumed -> when (val seed = consumed.seed) {
                        is EntryEditorSeed.Wifi -> AddWifiInitialContent.Scanned(seed.payload)
                    }
                    EntryEditorSeedConsumeResult.Missing,
                    EntryEditorSeedConsumeResult.AccessDenied,
                    -> AddWifiInitialContent.SeedUnavailable
                }
                EditorSeedTokenParseResult.Invalid -> AddWifiInitialContent.SeedUnavailable
            }
        }
        AddWifiEditorRoute(
            onBack = context.navigateBack,
            onSaved = context.navigateBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
            loadInitialContent = loadInitialContent,
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
            onOpenRelatedEntry = {
                context.navigateToRoute(AppRoute.Detail.createRoute(it))
            },
            onEditFullForm = { type ->
                VaultEditorDestinationRegistry.editRouteFor(type, entryId)?.let(context.navigateToRoute)
            },
        )
    }
}

private fun NavGraphBuilder.registerEditorEditDestination(
    navGraphBuilder: NavGraphBuilder,
    route: String,
    content: @Composable AnimatedVisibilityScope.(String) -> Unit,
) {
    navGraphBuilder.composable(
        route = route,
        arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
        content(entryId)
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
private fun vaultGraphSeedCoordinator(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
): EntryEditorSeedCoordinator {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoute.VaultGraph.route)
    }
    return hiltViewModel<EntryEditorSeedCoordinator>(owner)
}

private fun ShellNavigationContext.toVaultNavigation(
    seedCoordinator: EntryEditorSeedCoordinator,
) = VaultNavigation(
    navigateToRoute = navigateToRoute,
    navigateToSingleTopRoute = navigateToSingleTopRoute,
    seedStager = seedCoordinator,
)

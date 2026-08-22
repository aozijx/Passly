package com.aozijx.passly.presentation.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.action.rememberVaultActionProvider
import com.aozijx.passly.feature.vault.contract.VaultEffect
import com.aozijx.passly.feature.vault.contract.VaultUiAction
import com.aozijx.passly.feature.vault.display.VaultDisplayViewModel
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.vault.components.dialog.VaultDialogs
import com.aozijx.passly.presentation.vault.components.fab.VaultFab
import com.aozijx.passly.presentation.vault.components.list.VaultPagerContent
import com.aozijx.passly.presentation.vault.components.topbar.VaultTopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    vaultViewModel: VaultViewModel,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    requestReauthentication: (onSuccess: () -> Unit) -> Unit,
    requestSensitiveCopy: (onSuccess: () -> Unit) -> Unit,
    onUserInteraction: () -> Unit,
    onAddPassword: () -> Unit,
    onAddOtp: () -> Unit,
    onAddBankCard: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSettingsClick: () -> Unit = {},
    onShowDetail: (EntryListItem) -> Unit = {},
    isDatabaseInitializing: Boolean = false
) {
    val context = LocalContext.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayConfig by vaultDisplayViewModel.config.collectAsStateWithLifecycle()

    val entryCardPresentations = vaultDisplayConfig.style.entryCardPresentations
    var isFabVisible by remember { mutableStateOf(true) }

    BackHandler(enabled = uiState.isSearchActive) {
        vaultViewModel.onAction(VaultUiAction.SearchToggled(false))
    }

    val actionProvider = rememberVaultActionProvider(
        vaultViewModel = vaultViewModel,
        totpStates = vaultViewModel.totpStatesFlow,
        requestAuthentication = requestAuthentication,
        requestReauthentication = requestReauthentication,
        requestSensitiveCopy = requestSensitiveCopy,
        onUserInteraction = onUserInteraction,
        onShowDetail = onShowDetail,
        isFabVisible = { isFabVisible = it }
    )

    val initialQuickFilterIndex =
        uiState.visibleQuickFilters.indexOf(uiState.selectedQuickFilter).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialQuickFilterIndex) {
        uiState.visibleQuickFilters.size.coerceAtLeast(1)
    }

    LaunchedEffect(uiState.visibleQuickFilters, uiState.selectedQuickFilter) {
        if (uiState.visibleQuickFilters.isEmpty()) return@LaunchedEffect
        if (uiState.selectedQuickFilter !in uiState.visibleQuickFilters) {
            vaultViewModel.onAction(VaultUiAction.QuickFilterSelected(uiState.visibleQuickFilters.first()))
            return@LaunchedEffect
        }
        val targetIndex = uiState.visibleQuickFilters.indexOf(uiState.selectedQuickFilter)
        if (pagerState.settledPage != targetIndex && pagerState.pageCount > targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState, uiState.visibleQuickFilters) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            val newQuickFilter = uiState.visibleQuickFilters.getOrNull(page) ?: return@collect
            vaultViewModel.onAction(VaultUiAction.QuickFilterSelected(newQuickFilter))
        }
    }

    val activity = context as? FragmentActivity
    LaunchedEffect(scrollBehavior, vaultDisplayConfig.layout.hideSystemBars, activity) {
        activity ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (!vaultDisplayConfig.layout.hideSystemBars) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }

        snapshotFlow {
            when {
                scrollBehavior.state.collapsedFraction > 0.6f -> true
                scrollBehavior.state.collapsedFraction < 0.4f -> false
                else -> null
            }
        }.filterNotNull().distinctUntilChanged().collect { shouldHide ->
            if (shouldHide) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    LaunchedEffect(vaultViewModel, context) {
        vaultViewModel.effects.collect { effect ->
            val message = when (effect) {
                is VaultEffect.ShowError -> effect.message
                is VaultEffect.ShowToast -> effect.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (vaultDisplayConfig.layout.collapseTopBarOnScroll
                    || vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll
                    || vaultDisplayConfig.layout.hideSystemBars
                ) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            )
            .nestedScroll(actionProvider.fabScrollConnection),
        topBar = {
            Column {
                VaultTopBar(
                    uiState = uiState,
                    selectedQuickFilterIndex = pagerState.currentPage,
                    scrollBehavior = scrollBehavior,
                    onSettingsClick = onSettingsClick,
                    isStatusBarAutoHide = vaultDisplayConfig.layout.hideSystemBars,
                    isTopBarCollapsible = vaultDisplayConfig.layout.collapseTopBarOnScroll,
                    isQuickFilterBarCollapsible = vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
                    onSearchQueryChange = {
                        vaultViewModel.onAction(
                            VaultUiAction.SearchQueryChanged(
                                it
                            )
                        )
                    },
                    onToggleSearch = { vaultViewModel.onAction(VaultUiAction.SearchToggled(it)) },
                    onClearCategory = { vaultViewModel.onAction(VaultUiAction.ClearCategory) },
                    onToggleTotpVisibility = { vaultViewModel.onAction(VaultUiAction.ToggleShowTotpCode) },
                    onCategorySelected = { vaultViewModel.onAction(VaultUiAction.CategorySelected(it)) },
                    onSortSelected = { vaultViewModel.onAction(VaultUiAction.SortOptionSelected(it)) },
                    onSelectQuickFilter = {
                        vaultViewModel.onAction(
                            VaultUiAction.QuickFilterSelected(
                                it
                            )
                        )
                    }
                )

                if (uiState.isVaultItemsLoading || isDatabaseInitializing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            VaultFab(
                onAddTypeSelected = { type ->
                    when (type) {
                        AddType.PASSWORD -> onAddPassword()
                        AddType.TOTP -> onAddOtp()
                        AddType.BANK_CARD -> onAddBankCard()
                        else -> vaultViewModel.onAction(VaultUiAction.AddTypeSelected(type))
                    }
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isVisible = isFabVisible
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        VaultPagerContent(
            pagerState = pagerState,
            uiState = uiState,
            entryCardPresentations = entryCardPresentations,
            hierarchyDisplayMode = vaultDisplayConfig.style.entryHierarchyDisplayMode,
            totpStates = vaultViewModel.totpStatesFlow,
            swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction,
            swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction,
            isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
            onSwipeTriggered = actionProvider.onSwipeTriggered,
            onItemClick = { onShowDetail(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    VaultDialogs(
        uiState = uiState,
        onDismissAddType = { vaultViewModel.onAction(VaultUiAction.AddTypeSelected(null)) },
        onConfirmDelete = { vaultViewModel.onAction(VaultUiAction.ConfirmDelete) },
        onDismissDelete = { vaultViewModel.onAction(VaultUiAction.ItemToDeleteSelected(null)) },
        requestAuthentication = requestAuthentication,
    )
}

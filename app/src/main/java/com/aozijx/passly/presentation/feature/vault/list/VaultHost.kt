package com.aozijx.passly.presentation.feature.vault.list

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
import androidx.paging.map
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.feature.vault.list.VaultViewModel
import com.aozijx.passly.presentation.feature.vault.list.action.rememberVaultActionProvider
import com.aozijx.passly.presentation.feature.vault.list.VaultEffect
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.display.VaultDisplayViewModel
import com.aozijx.passly.presentation.ui.vault.list.component.dialog.VaultDialogs
import com.aozijx.passly.presentation.ui.vault.list.component.fab.VaultFab
import com.aozijx.passly.presentation.ui.vault.list.component.list.VaultPagerContent
import com.aozijx.passly.presentation.ui.vault.list.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHost(
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

    val entryCardPresentations = vaultDisplayConfig.style.entryCardPresentations.map { it.toUiModel() }
    val renderState = uiState.toUiModel()
    var isFabVisible by remember { mutableStateOf(true) }

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
    val entryPages = remember(vaultViewModel, actionProvider, onShowDetail) {
        com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel.entries
            .associateWith { filter ->
                vaultViewModel.entries(filter.toFeatureModel()).map { pagingData ->
                    pagingData.map { item ->
                        item.toUiModel(
                            events = object : VaultListItemEventHandler {
                                override fun onClick() = onShowDetail(item)
                                override fun onSwipe(action: com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel) {
                                    actionProvider.onSwipeTriggered(action.toFeatureModel(), item)
                                }
                            },
                        )
                    }
                }
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

    com.aozijx.passly.presentation.ui.vault.list.VaultScreen(
        state = renderState,
        scrollBehavior = scrollBehavior,
        entryPages = entryPages,
        cardPresentations = entryCardPresentations,
        otpState = { id -> vaultViewModel.totpStatesFlow.map { it[id]?.toUiModel() } },
        swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction.toUiModel(),
        swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction.toUiModel(),
        isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
        fabScrollConnection = actionProvider.fabScrollConnection,
        isFabVisible = isFabVisible,
        collapseTopBarOnScroll = vaultDisplayConfig.layout.collapseTopBarOnScroll,
        collapseQuickFilterBarOnScroll = vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
        hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        requestAuthentication = requestAuthentication,
        onSettingsClick = onSettingsClick,
        onSearchQueryChange = { vaultViewModel.onAction(VaultUiAction.SearchQueryChanged(it)) },
        onSearchToggled = { vaultViewModel.onAction(VaultUiAction.SearchToggled(it)) },
        onClearCategory = { vaultViewModel.onAction(VaultUiAction.ClearCategory) },
        onToggleTotpVisibility = { vaultViewModel.onAction(VaultUiAction.ToggleShowTotpCode) },
        onCategorySelected = { vaultViewModel.onAction(VaultUiAction.CategorySelected(it)) },
        onSortSelected = { vaultViewModel.onAction(VaultUiAction.SortOptionSelected(it.toFeatureModel())) },
        onQuickFilterSelected = { vaultViewModel.onAction(VaultUiAction.QuickFilterSelected(it.toFeatureModel())) },
        onAddTypeSelected = { type ->
            when (type) {
                VaultAddTypeUiModel.PASSWORD -> onAddPassword()
                VaultAddTypeUiModel.TOTP -> onAddOtp()
                VaultAddTypeUiModel.BANK_CARD -> onAddBankCard()
                else -> vaultViewModel.onAction(VaultUiAction.AddTypeSelected(type.toFeatureModel()))
            }
        },
        onDismissAddType = { vaultViewModel.onAction(VaultUiAction.AddTypeSelected(null)) },
        onConfirmDelete = { vaultViewModel.onAction(VaultUiAction.ConfirmDelete) },
        onDismissDelete = { vaultViewModel.onAction(VaultUiAction.ItemToDeleteSelected(null)) },
        isDatabaseInitializing = isDatabaseInitializing,
    )
}

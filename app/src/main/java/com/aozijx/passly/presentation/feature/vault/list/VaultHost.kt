package com.aozijx.passly.presentation.feature.vault.list

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.vault.list.action.rememberVaultActionProvider
import com.aozijx.passly.presentation.feature.vault.list.display.VaultDisplayViewModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

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
    onShowDetail: (String) -> Unit = {},
    isDatabaseInitializing: Boolean = false
) {
    val context = LocalContext.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayConfig by vaultDisplayViewModel.config.collectAsStateWithLifecycle()

    val entryCardPresentations =
        vaultDisplayConfig.style.entryCardPresentations.map { it.toUiModel() }
    var isFabVisible by remember { mutableStateOf(true) }
    val renderState = rememberVaultListScreenUiModel(
        uiState.toUiModel(
            display = VaultListDisplayUiModel(
                cardPresentations = entryCardPresentations,
                swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction.toUiModel(),
                swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction.toUiModel(),
                isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
                isFabVisible = isFabVisible,
                collapseTopBarOnScroll = vaultDisplayConfig.layout.collapseTopBarOnScroll,
                collapseQuickFilterBarOnScroll =
                    vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
                hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
            ),
            isDatabaseInitializing = isDatabaseInitializing,
        ),
    )
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
    val entryPages = remember(vaultViewModel) {
        com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel.entries
            .associateWith { filter -> vaultViewModel.entries(filter.toFeatureModel()) }
    }
    val listBindings = rememberVaultListBindings(
        entryPages = entryPages,
        onItemClick = { item -> onShowDetail(item.id) },
        onItemSwipe = { item, action ->
            actionProvider.onSwipeTriggered(action.toFeatureModel(), item)
        },
    )
    val eventHandler = rememberVaultListEventHandler(
        onEvent = { event ->
            when (event) {
                VaultListEvent.SettingsClicked -> onSettingsClick()
                is VaultListEvent.SearchQueryChanged -> vaultViewModel.onAction(
                    VaultUiAction.SearchQueryChanged(event.query),
                )

                is VaultListEvent.SearchToggled -> vaultViewModel.onAction(
                    VaultUiAction.SearchToggled(event.active),
                )

                VaultListEvent.ClearCategory -> vaultViewModel.onAction(VaultUiAction.ClearCategory)
                VaultListEvent.ToggleTotpVisibility -> vaultViewModel.onAction(
                    VaultUiAction.ToggleShowTotpCode,
                )

                is VaultListEvent.CategorySelected -> vaultViewModel.onAction(
                    VaultUiAction.CategorySelected(event.category),
                )

                is VaultListEvent.SortSelected -> vaultViewModel.onAction(
                    VaultUiAction.SortOptionSelected(event.sort.toFeatureModel()),
                )

                is VaultListEvent.QuickFilterSelected -> vaultViewModel.onAction(
                    VaultUiAction.QuickFilterSelected(event.filter.toFeatureModel()),
                )

                is VaultListEvent.AddTypeSelected -> when (event.type) {
                    VaultAddTypeUiModel.PASSWORD -> onAddPassword()
                    VaultAddTypeUiModel.TOTP -> onAddOtp()
                    VaultAddTypeUiModel.BANK_CARD -> onAddBankCard()
                    else -> vaultViewModel.onAction(
                        VaultUiAction.AddTypeSelected(event.type.toFeatureModel()),
                    )
                }

                VaultListEvent.DismissAddType -> vaultViewModel.onAction(
                    VaultUiAction.AddTypeSelected(null),
                )

                VaultListEvent.ConfirmDelete -> vaultViewModel.onAction(
                    VaultUiAction.ConfirmDelete,
                )

                VaultListEvent.DismissDelete -> vaultViewModel.onAction(
                    VaultUiAction.ItemToDeleteSelected(null),
                )
            }
        },
        requestAuthentication = requestAuthentication,
    )
    val otpStateProvider = rememberVaultOtpStateProvider(
        states = vaultViewModel.totpStatesFlow,
        onSubscribe = vaultViewModel::subscribeVisibleOtp,
        onUnsubscribe = vaultViewModel::unsubscribeVisibleOtp,
    )

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
        entryPages = listBindings.entryPages,
        itemEventHandler = listBindings.eventHandler,
        otpStateProvider = otpStateProvider,
        fabScrollConnection = actionProvider.fabScrollConnection,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        eventHandler = eventHandler,
    )
}

@Composable
internal fun rememberVaultListScreenUiModel(
    mapped: VaultListScreenUiModel,
): VaultListScreenUiModel {
    val toolbar = remember(mapped.toolbar) { mapped.toolbar }
    val navigation = remember(mapped.navigation) { mapped.navigation }
    val content = remember(mapped.content) { mapped.content }
    val dialogs = remember(mapped.dialogs) { mapped.dialogs }
    val layout = remember(mapped.layout) { mapped.layout }
    return remember(toolbar, navigation, content, dialogs, layout) {
        VaultListScreenUiModel(toolbar, navigation, content, dialogs, layout)
    }
}

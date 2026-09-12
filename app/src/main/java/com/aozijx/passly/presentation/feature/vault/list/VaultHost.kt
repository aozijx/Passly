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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.action.CopyFieldLabelProvider
import com.aozijx.passly.presentation.feature.vault.list.action.VaultCopyRequest
import com.aozijx.passly.presentation.feature.vault.list.action.handleSwipeAction
import com.aozijx.passly.presentation.feature.vault.list.action.resolveCopyRequest
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
    onAddPassword: () -> Unit,
    onAddOtp: () -> Unit,
    onAddBankCard: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSettingsClick: () -> Unit = {},
    onShowDetail: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayConfig by vaultDisplayViewModel.config.collectAsStateWithLifecycle()

    val entryCardPresentations =
        vaultDisplayConfig.style.entryCardPresentations.map { it.toUiModel() }
    val renderState = rememberVaultListScreenUiModel(
        uiState.toUiModel(
            display = VaultListDisplayUiModel(
                cardPresentations = entryCardPresentations,
                swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction.toUiModel(),
                swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction.toUiModel(),
                isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
                collapseTopBarOnScroll = vaultDisplayConfig.layout.collapseTopBarOnScroll,
                collapseQuickFilterBarOnScroll =
                    vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
                hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
            ),
        ),
    )
    val listBindings = rememberVaultListBindings(
        entries = vaultViewModel.entries,
        onItemClick = { item -> onShowDetail(item.id) },
        onItemSwipe = { item, action ->
            handleSwipeAction(
                actionType = action.toFeatureModel(),
                item = item,
                onDeleteAuthRequired = requestReauthentication,
                onCopyAuthRequired = requestSensitiveCopy,
                onQuickDelete = { entryId ->
                    vaultViewModel.onAction(VaultUiAction.QuickDelete(entryId))
                },
                onShowDetail = onShowDetail,
                onCopy = { fieldKey ->
                    when (val request = resolveCopyRequest(item, fieldKey)) {
                        is VaultCopyRequest.Field -> requestAuthentication {
                            vaultViewModel.onAction(
                                VaultUiAction.CopyField(request.entryId, request.fieldKey),
                            )
                        }

                        is VaultCopyRequest.Otp -> vaultViewModel.onAction(
                            VaultUiAction.CopyOtp(request.entryId),
                        )
                    }
                },
            )
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

                is VaultListEvent.FilterToggled -> vaultViewModel.onAction(
                    VaultUiAction.FilterToggled(event.filter?.toFeatureModel()),
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

    val totpLabel = stringResource(R.string.vault_detail_totp_label)
    val fieldCopiedFormat = stringResource(R.string.field_copy_success_message)
    LaunchedEffect(vaultViewModel, context, totpLabel, fieldCopiedFormat) {
        vaultViewModel.effects.collect { effect ->
            val message = when (effect) {
                is VaultEffect.ShowError -> effect.message
                is VaultEffect.ShowToast -> effect.message
                is VaultEffect.FieldCopied -> fieldCopiedFormat.format(
                    CopyFieldLabelProvider.getCopyLabel(effect.fieldKey),
                )

                VaultEffect.OtpCopied -> fieldCopiedFormat.format(totpLabel)
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
        entries = listBindings.entries,
        itemEventHandler = listBindings.eventHandler,
        otpStateProvider = otpStateProvider,
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

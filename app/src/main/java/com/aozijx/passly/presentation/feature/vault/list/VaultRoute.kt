package com.aozijx.passly.presentation.feature.vault.list

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.action.CopyFieldLabelProvider
import com.aozijx.passly.presentation.feature.vault.list.action.VaultCopyRequest
import com.aozijx.passly.presentation.feature.vault.list.action.handleSwipeAction
import com.aozijx.passly.presentation.feature.vault.list.action.resolveCopyRequest
import com.aozijx.passly.presentation.feature.vault.list.display.VaultDisplayViewModel
import com.aozijx.passly.presentation.ui.vault.list.VaultScreen
import com.aozijx.passly.presentation.ui.vault.list.VaultSystemBarsEffect
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListDisplayUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultRoute(
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

    val display = remember(vaultDisplayConfig) {
        VaultListDisplayUiModel(
            cardPresentations = vaultDisplayConfig.style.entryCardPresentations.map {
                it.toUiModel()
            },
            swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction.toUiModel(),
            swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction.toUiModel(),
            isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
            collapseTopBarOnScroll = vaultDisplayConfig.layout.collapseTopBarOnScroll,
            collapseQuickFilterBarOnScroll =
                vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
            hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
        )
    }
    val renderState = uiState.toUiModel(display)
    val itemEventHandler = rememberVaultListItemEventHandler(
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
    val routeCallbacks = VaultListRouteCallbacks(
        onSettingsClick = onSettingsClick,
        onAddPassword = onAddPassword,
        onAddOtp = onAddOtp,
        onAddBankCard = onAddBankCard,
    )
    val eventHandler = rememberVaultListEventHandler(
        onEvent = { event ->
            dispatchVaultListEvent(event, vaultViewModel::onAction, routeCallbacks)
        },
        requestAuthentication = requestAuthentication,
    )
    val otpStateProvider = rememberVaultOtpStateProvider(
        states = vaultViewModel.totpStatesFlow,
        onSubscribe = vaultViewModel::subscribeVisibleOtp,
        onUnsubscribe = vaultViewModel::unsubscribeVisibleOtp,
    )

    VaultSystemBarsEffect(
        scrollBehavior = scrollBehavior,
        hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
    )
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

    VaultScreen(
        state = renderState,
        scrollBehavior = scrollBehavior,
        entries = vaultViewModel.entries,
        itemEventHandler = itemEventHandler,
        otpStateProvider = otpStateProvider,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        eventHandler = eventHandler,
    )
}

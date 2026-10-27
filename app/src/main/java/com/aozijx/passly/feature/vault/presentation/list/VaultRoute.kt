package com.aozijx.passly.feature.vault.presentation.list

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.ScannerImport
import com.aozijx.passly.feature.scanner.presentation.ScannerImportHandlingResult
import com.aozijx.passly.feature.scanner.presentation.ScannerPurpose
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.vault.presentation.common.copyLabelRes
import com.aozijx.passly.feature.vault.presentation.list.action.VaultCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.action.handleSwipeAction
import com.aozijx.passly.feature.vault.presentation.list.action.resolveCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayViewModel
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultScreen
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultSystemBarsEffect
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemAction
import com.aozijx.passly.feature.vault.presentation.navigation.VaultDestinationRequest
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigation
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultRoute(
    vaultViewModel: VaultViewModel,
    navigation: VaultNavigation,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var overlay by rememberSaveable { mutableStateOf(VaultOverlay.None) }

    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayState by vaultDisplayViewModel.config.collectAsStateWithLifecycle()
    val vaultDisplayConfig = when (val state = vaultDisplayState) {
        VaultDisplayUiState.Loading -> return
        is VaultDisplayUiState.Ready -> state
    }

    val display = remember(vaultDisplayConfig) {
        VaultListDisplayUiModel(
            cardPresentations = vaultDisplayConfig.style.entryCardPresentations.map {
                it.toUiModel()
            },
            swipeLeftAction = vaultDisplayConfig.interaction.swipeLeftAction,
            swipeRightAction = vaultDisplayConfig.interaction.swipeRightAction,
            isSwipeEnabled = vaultDisplayConfig.interaction.isSwipeEnabled,
            isPullToSearchEnabled = vaultDisplayConfig.interaction.isPullToSearchEnabled,
            collapseTopBarOnScroll = vaultDisplayConfig.layout.collapseTopBarOnScroll,
            collapseQuickFilterBarOnScroll =
                vaultDisplayConfig.layout.collapseQuickFilterBarOnScroll,
            hideSystemBars = vaultDisplayConfig.layout.hideSystemBars,
        )
    }
    val renderState = uiState.toUiModel(display) ?: return
    val onItemAction: (VaultListItemAction) -> Unit = { action ->
        when (action) {
            is VaultListItemAction.Clicked -> navigation.navigate(
                VaultDestinationRequest.OpenDetail(action.item.id),
            )
            is VaultListItemAction.Swiped -> {
                val item = action.item
                handleSwipeAction(
                    actionType = action.action,
                    item = item,
                    onQuickDelete = { entryId ->
                        vaultViewModel.onAction(VaultUiAction.QuickDelete(entryId))
                    },
                    onCopy = { fieldKey ->
                        when (val request = resolveCopyRequest(item, fieldKey)) {
                            is VaultCopyRequest.Field -> vaultViewModel.onAction(
                                VaultUiAction.CopyField(
                                    entryId = request.entryId,
                                    entryType = request.entryType,
                                    fieldKey = request.fieldKey,
                                ),
                            )

                            is VaultCopyRequest.Otp -> vaultViewModel.onAction(
                                VaultUiAction.CopyOtp(request.entryId),
                            )
                        }
                    },
                )
            }
        }
    }
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
    LaunchedEffect(vaultViewModel, context, resources, totpLabel, fieldCopiedFormat) {
        vaultViewModel.effects.collect { effect ->
            val message = when (effect) {
                is VaultEffect.ShowError -> resources.getString(effect.message.resourceId)
                is VaultEffect.FieldCopied -> fieldCopiedFormat.format(
                    resources.getString(effect.fieldKey.copyLabelRes()),
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
        onItemAction = onItemAction,
        otpStateProvider = otpStateProvider,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onAction = vaultViewModel::onAction,
        onRouteRequest = { request ->
            when (request) {
                VaultRouteRequest.OpenScanner -> overlay = VaultOverlay.Scanner
                VaultRouteRequest.OpenSettings -> {
                    navigation.navigate(VaultDestinationRequest.OpenSettings)
                }
            }
        },
        onAddTypeSelected = { type ->
            navigation.navigate(VaultDestinationRequest.OpenEditor(type))
        },
    )

    when (overlay) {
        VaultOverlay.None -> Unit
        VaultOverlay.Scanner -> ScannerRoute(
            purpose = ScannerPurpose.General,
            onImport = { scannerImport ->
                when (scannerImport) {
                    is ScannerImport.Otp -> {
                        if (vaultViewModel.importScannedOtp(scannerImport.config)) {
                            ScannerImportHandlingResult.Applied
                        } else {
                            ScannerImportHandlingResult.Rejected
                        }
                    }
                    is ScannerImport.Wifi -> when (
                        navigation.navigate(
                            VaultDestinationRequest.EditScannedWifi(scannerImport.payload),
                        )
                    ) {
                        VaultNavigationResult.Navigated -> ScannerImportHandlingResult.Applied
                        VaultNavigationResult.UnsupportedDestination,
                        VaultNavigationResult.AccessDenied,
                        -> ScannerImportHandlingResult.Rejected
                    }
                }
            },
            onDismiss = { overlay = VaultOverlay.None },
        )
    }
}

private enum class VaultOverlay {
    None,
    Scanner,
}

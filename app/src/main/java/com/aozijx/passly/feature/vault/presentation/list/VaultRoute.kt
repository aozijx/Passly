package com.aozijx.passly.feature.vault.presentation.list

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerOverlay
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerPrimaryActionLabel
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultHandling
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPlan
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultTextInteraction
import com.aozijx.passly.feature.vault.presentation.ScannedOtpImportViewModel
import com.aozijx.passly.feature.vault.presentation.common.copyLabelRes
import com.aozijx.passly.feature.vault.presentation.list.action.VaultCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.action.handleSwipeAction
import com.aozijx.passly.feature.vault.presentation.list.action.resolveCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayUiState
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayViewModel
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultScreen
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultSystemBarsEffect
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemAction
import com.aozijx.passly.feature.vault.presentation.navigation.VaultDestinationRequest
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigation
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigationResult
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent
import java.net.URI

@Composable
internal fun VaultRoute(
    vaultViewModel: VaultViewModel,
    scannedOtpImportViewModel: ScannedOtpImportViewModel,
    navigation: VaultNavigation,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var scannerOverlayVisible by remember { mutableStateOf(false) }
    DisposableEffect(vaultViewModel, scannerOverlayVisible) {
        val owner =
            if (scannerOverlayVisible) null else vaultViewModel.onPageEntered()
        onDispose {
            owner?.let(vaultViewModel::onPageExited)
        }
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vaultViewModel.refreshVisibleOtp()
    }
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val vaultDisplayViewModel: VaultDisplayViewModel = hiltViewModel()
    val vaultDisplayState by vaultDisplayViewModel.config.collectAsStateWithLifecycle()
    val librarySettingsState by vaultViewModel.librarySettingsState.collectAsStateWithLifecycle()
    val vaultDisplayConfig =
        when (val state = vaultDisplayState) {
            VaultDisplayUiState.Loading -> {
                VaultPageLoading()
                return
            }

            is VaultDisplayUiState.Failed -> {
                SettingsReadFailureContent(
                    onRetry = {
                        vaultDisplayViewModel.retry()
                        vaultViewModel.retryLibrarySettings()
                    },
                )
                return
            }

            is VaultDisplayUiState.Ready -> {
                state
            }
        }
    val librarySettings =
        when (val state = librarySettingsState) {
            RecoverableReadState.Loading -> {
                VaultPageLoading()
                return
            }

            is RecoverableReadState.Failed -> {
                SettingsReadFailureContent(
                    onRetry = {
                        vaultDisplayViewModel.retry()
                        vaultViewModel.retryLibrarySettings()
                    },
                )
                return
            }

            is RecoverableReadState.Ready -> {
                state.value
            }
        }

    val renderState = uiState.toUiModel(vaultDisplayConfig, librarySettings.sort)
    val onItemAction: (VaultListItemAction) -> Unit = { action ->
        when (action) {
            is VaultListItemAction.Clicked -> {
                navigation.navigate(
                    VaultDestinationRequest.OpenDetail(action.item.id),
                )
            }

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
                            is VaultCopyRequest.Field -> {
                                vaultViewModel.onAction(
                                    VaultUiAction.CopyField(
                                        entryId = request.entryId,
                                        entryType = request.entryType,
                                        fieldKey = request.fieldKey,
                                    ),
                                )
                            }

                            is VaultCopyRequest.Otp -> {
                                vaultViewModel.onAction(
                                    VaultUiAction.CopyOtp(request.entryId),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
    val otpStateProvider =
        rememberVaultOtpStateProvider(
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
    LaunchedEffect(vaultViewModel, lifecycle, context, resources, totpLabel, fieldCopiedFormat) {
        collectPageEffects(lifecycle, vaultViewModel.effects) { effect ->
            val message =
                when (effect) {
                    is VaultEffect.ShowError -> {
                        resources.getString(effect.message.resourceId)
                    }

                    is VaultEffect.FieldCopied -> {
                        fieldCopiedFormat.format(
                            resources.getString(effect.fieldKey.copyLabelRes()),
                        )
                    }

                    VaultEffect.OtpCopied -> {
                        fieldCopiedFormat.format(totpLabel)
                    }
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
                VaultRouteRequest.OpenScanner -> {
                    scannerOverlayVisible = true
                }

                VaultRouteRequest.OpenSettings -> {
                    navigation.navigate(VaultDestinationRequest.OpenSettings)
                }
            }
        },
        onAddTypeSelected = { type ->
            navigation.navigate(VaultDestinationRequest.OpenEditor(type.featureType))
        },
    )

    ScannerOverlay(
        visible = scannerOverlayVisible,
        resolveResult = { rawValue ->
            when (val code = resolveVaultScannedCode(rawValue)) {
                is VaultScannedCode.Otp -> {
                    ScannerResultPlan(
                        presentation =
                            ScannerResultPresentation(
                                resultTextInteraction = ScannerResultTextInteraction.ReadOnly,
                                primaryActionLabel = ScannerPrimaryActionLabel.Save,
                            ),
                        confirm = {
                            if (scannedOtpImportViewModel.importScannedOtp(code.config)) {
                                ScannerResultHandling.Handled
                            } else {
                                ScannerResultHandling.Rejected
                            }
                        },
                    )
                }

                is VaultScannedCode.Wifi -> {
                    ScannerResultPlan(
                        presentation = ScannerResultPresentation.DEFAULT,
                        confirm = {
                            when (val result = navigation.navigate(VaultDestinationRequest.EditScannedWifi(code.payload))) {
                                VaultNavigationResult.Navigated -> {
                                    ScannerResultHandling.Handled
                                }

                                VaultNavigationResult.UnsupportedDestination,
                                VaultNavigationResult.AccessDenied,
                                -> {
                                    ScannerResultHandling.Rejected
                                }

                                is VaultNavigationResult.NavigationFailed -> {
                                    ScannerResultHandling.Failed(result.failure)
                                }
                            }
                        },
                    )
                }

                is VaultScannedCode.WebLink -> {
                    ScannerResultPlan(
                        presentation = ScannerResultPresentation.DEFAULT,
                        confirm = {
                            if (context.openScannedWebLink(code.uri)) {
                                ScannerResultHandling.Handled
                            } else {
                                Toast.makeText(context, R.string.scanner_open_link_failed, Toast.LENGTH_SHORT).show()
                                ScannerResultHandling.Rejected
                            }
                        },
                    )
                }

                VaultScannedCode.Unsupported -> {
                    ScannerResultPlan(
                        presentation = ScannerResultPresentation.DEFAULT,
                        confirm = { ScannerResultHandling.Rejected },
                    )
                }
            }
        },
        onDismiss = { scannerOverlayVisible = false },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VaultPageLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

private fun android.content.Context.openScannedWebLink(uri: URI): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toString().toUri()))
        true
    } catch (_: Exception) {
        false
    }

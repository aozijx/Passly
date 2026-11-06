package com.aozijx.passly.feature.vault.presentation.list

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadParser
import com.aozijx.passly.feature.scanner.presentation.ScannerResultHandling
import com.aozijx.passly.feature.scanner.presentation.ScannerPrimaryActionLabel
import com.aozijx.passly.feature.scanner.presentation.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.ScannerResultPlan
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.vault.presentation.ScannedOtpImportViewModel
import com.aozijx.passly.feature.vault.presentation.common.copyLabelRes
import com.aozijx.passly.feature.vault.presentation.list.action.VaultCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.action.handleSwipeAction
import com.aozijx.passly.feature.vault.presentation.list.action.resolveCopyRequest
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayUiState
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayViewModel
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultScreen
import com.aozijx.passly.feature.vault.presentation.list.ui.VaultSystemBarsEffect
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemAction
import com.aozijx.passly.feature.vault.presentation.navigation.VaultDestinationRequest
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigation
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigationResult
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent
import java.net.URI
import java.net.URISyntaxException

@Composable
internal fun VaultRoute(
    vaultViewModel: VaultViewModel,
    scannedOtpImportViewModel: ScannedOtpImportViewModel,
    navigation: VaultNavigation,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isEffectsPage: Boolean = true,
) {
    var overlay by rememberSaveable { mutableStateOf(VaultOverlay.None) }
    DisposableEffect(vaultViewModel, isEffectsPage, overlay) {
        val active = isEffectsPage && overlay == VaultOverlay.None
        val owner = if (active) vaultViewModel.onPageEntered() else null
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
    when (librarySettingsState) {
        RecoverableReadState.Loading -> {
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

        is RecoverableReadState.Ready -> {}
    }

    val display =
        remember(vaultDisplayConfig) {
            VaultListDisplayUiModel(
                cardPresentations =
                    vaultDisplayConfig.style.entryCardPresentations.map {
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
                    overlay = VaultOverlay.Scanner
                }

                VaultRouteRequest.OpenSettings -> {
                    navigation.navigate(VaultDestinationRequest.OpenSettings)
                }
            }
        },
        onAddTypeSelected = { type ->
            navigation.navigate(VaultDestinationRequest.OpenEditor(type))
        },
    )

    AnimatedVisibility(
        visible = overlay == VaultOverlay.Scanner,
        enter =
            fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it },
                ),
        exit =
            fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { it },
                ),
    ) {
        ScannerRoute(
            resolveResult = resolve@{ rawValue ->
                val otpConfig = (OtpAuthUriCodec.parse(rawValue) as? OtpUriParseResult.Parsed)?.config
                if (otpConfig != null) {
                    return@resolve ScannerResultPlan(
                        presentation = ScannerResultPresentation(
                            showCopyAction = false,
                            primaryActionLabel = ScannerPrimaryActionLabel.Save,
                        ),
                        confirm = {
                            if (scannedOtpImportViewModel.importScannedOtp(otpConfig)) {
                                ScannerResultHandling.Handled
                            } else {
                                ScannerResultHandling.Rejected
                            }
                        },
                    )
                }

                val wifiPayload =
                    (WifiQrPayloadParser.parse(rawValue) as? WifiQrParseResult.Parsed)?.payload
                if (wifiPayload != null) {
                    return@resolve ScannerResultPlan(
                        presentation = ScannerResultPresentation.DEFAULT,
                        confirm = {
                            when (navigation.navigate(VaultDestinationRequest.EditScannedWifi(wifiPayload))) {
                                VaultNavigationResult.Navigated -> ScannerResultHandling.Handled
                                VaultNavigationResult.UnsupportedDestination,
                                VaultNavigationResult.AccessDenied,
                                -> ScannerResultHandling.Rejected
                            }
                        },
                    )
                }

                val webUri =
                    rawValue.toScannedWebUri()
                if (webUri != null) {
                    return@resolve ScannerResultPlan(
                        presentation = ScannerResultPresentation.DEFAULT,
                        confirm = {
                            if (context.openScannedWebLink(webUri)) {
                                ScannerResultHandling.Handled
                            } else {
                                Toast.makeText(context, R.string.scanner_open_link_failed, Toast.LENGTH_SHORT).show()
                                ScannerResultHandling.Rejected
                            }
                        },
                    )
                }

                ScannerResultPlan(
                    presentation = ScannerResultPresentation.DEFAULT,
                    confirm = { ScannerResultHandling.Rejected },
                )
            },
            onDismiss = { overlay = VaultOverlay.None },
        )
    }
}

private fun String.toScannedWebUri(): URI? =
    try {
        URI(this).takeIf { uri ->
            (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
                !uri.host.isNullOrBlank()
        }
    } catch (_: URISyntaxException) {
        null
    }

private fun android.content.Context.openScannedWebLink(uri: URI): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toString().toUri()))
        true
    } catch (_: Exception) {
        false
    }

private enum class VaultOverlay {
    None,
    Scanner,
}

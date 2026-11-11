package com.aozijx.passly.feature.scanner.presentation.route

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.action.ScannerScreenAction
import com.aozijx.passly.feature.scanner.presentation.action.ScannerUiAction
import com.aozijx.passly.feature.scanner.presentation.action.resolveScannerScreenAction
import com.aozijx.passly.feature.scanner.presentation.camera.ScannerCameraHost
import com.aozijx.passly.feature.scanner.presentation.image.ImageRef
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPlan
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultOutcome
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultSession
import com.aozijx.passly.feature.scanner.presentation.state.ScannerFeedback
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.state.mapScannerCameraState
import com.aozijx.passly.feature.scanner.presentation.state.mapScannerResultActionState
import com.aozijx.passly.feature.scanner.presentation.state.mapScannerScreenState
import com.aozijx.passly.feature.scanner.presentation.ui.ScannerScreen
import com.aozijx.passly.feature.scanner.presentation.viewfinder.ScannerViewfinderFrame
import com.aozijx.passly.feature.scanner.presentation.viewmodel.ScannerViewModel
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker
import kotlinx.coroutines.launch

@Composable
internal fun ScannerRoute(
    visible: Boolean,
    resolveResult: (String) -> ScannerResultPlan,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ScannerViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = state
    val scope = rememberCoroutineScope()
    val resultSession = remember { ScannerResultSession() }
    val resultPhase by resultSession.phase.collectAsStateWithLifecycle()
    val resultActions = mapScannerResultActionState(resultPhase)
    val resultValue =
        when (currentState) {
            is ScannerUiState.ShowingResult -> currentState.result.rawValue

            is ScannerUiState.Scanning,
            ScannerUiState.ProcessingImage,
            ScannerUiState.Stopped,
            -> null
        }
    val resultPlan = remember(resultValue) { resultValue?.let(resolveResult) }
    DisposableEffect(resultSession) {
        onDispose { resultSession.close() }
    }
    val context = LocalContext.current
    val copySucceededMessage = stringResource(R.string.scanner_copy_succeeded)
    val rejectedMessage = stringResource(R.string.scanner_result_rejected)
    val actionFailedMessage = stringResource(R.string.scanner_result_action_failed)
    val imageNoBarcodeMessage = stringResource(R.string.scanner_image_no_barcode)
    val imageDecodeFailedMessage = stringResource(R.string.scanner_image_decode_failed)
    DisposableEffect(viewModel, visible) {
        if (visible) viewModel.onAction(ScannerUiAction.StartScanning)
        onDispose {
            if (visible) viewModel.onAction(ScannerUiAction.StopScanning)
        }
    }
    val pickPhoto =
        rememberImagePicker { uri, _ ->
            viewModel.onAction(ScannerUiAction.DecodeImage(ImageRef(uri.toString())))
        }

    fun dismissScanner() {
        resultSession.close()
        onDismiss()
    }

    LaunchedEffect(viewModel, lifecycleOwner, context, copySucceededMessage, imageNoBarcodeMessage, imageDecodeFailedMessage) {
        collectScannerRouteFeedback(
            lifecycle = lifecycleOwner.lifecycle,
            uiState = viewModel.uiState,
            onHostStarted = { viewModel.onAction(ScannerUiAction.HostStarted) },
            onHostStopped = { viewModel.onAction(ScannerUiAction.HostStopped) },
            onFeedbackConsumed = { viewModel.onAction(ScannerUiAction.FeedbackConsumed(it)) },
            onFeedback = { feedback ->
                if (feedback.kind == ScannerFeedback.Kind.ScanSucceeded) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                        .defaultVibrator
                        .vibrate(
                            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE),
                        )
                    return@collectScannerRouteFeedback
                }
                val message =
                    when (feedback.kind) {
                        ScannerFeedback.Kind.CopySucceeded -> copySucceededMessage
                        ScannerFeedback.Kind.ImageNoBarcode -> imageNoBarcodeMessage
                        ScannerFeedback.Kind.ImageDecodeFailed -> imageDecodeFailedMessage
                        ScannerFeedback.Kind.ScanSucceeded -> return@collectScannerRouteFeedback
                    }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }

    fun handleScreenAction(action: ScannerScreenAction) {
        when (resolveScannerScreenAction(action, resultActions)) {
            null -> {
                Unit
            }

            ScannerScreenAction.Dismiss -> {
                dismissScanner()
            }

            ScannerScreenAction.PickPhoto -> {
                viewModel.onAction(ScannerUiAction.StartScanning)
                pickPhoto(ImageType.SCREEN)
            }

            ScannerScreenAction.ScanAgain -> {
                viewModel.onAction(ScannerUiAction.StartScanning)
            }

            ScannerScreenAction.CopyResult -> {
                viewModel.onAction(ScannerUiAction.CopyText)
            }

            ScannerScreenAction.ConfirmResult -> {
                resultPlan?.let { plan ->
                    scope.launch {
                        when (resultSession.confirm { plan.confirm() }) {
                            ScannerResultOutcome.Handled -> {
                                dismissScanner()
                            }

                            ScannerResultOutcome.Ignored -> {
                                Unit
                            }

                            ScannerResultOutcome.Rejected -> {
                                Toast.makeText(context, rejectedMessage, Toast.LENGTH_SHORT).show()
                            }

                            is ScannerResultOutcome.Failed -> {
                                Toast.makeText(context, actionFailedMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = visible) { handleScreenAction(ScannerScreenAction.Dismiss) }

    val screenState =
        mapScannerScreenState(
            scanner = currentState,
            resultActions = resultActions,
            presentation = resultPlan?.presentation ?: ScannerResultPresentation.DEFAULT,
        )
    val cameraState = mapScannerCameraState(currentState)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        val density = LocalDensity.current
        val viewfinderFrame =
            remember(maxWidth, maxHeight, density) {
                ScannerViewfinderFrame.fitContainer(maxWidth, maxHeight, density)
            }
        ScannerCameraHost(
            onBarcodeDetected = { viewModel.onAction(ScannerUiAction.BarcodeDetected(it)) },
            onPermissionDenied = ::dismissScanner,
            onFailure = { viewModel.onAction(ScannerUiAction.CameraFailure(it)) },
            cameraState = cameraState,
            viewfinderFrame = viewfinderFrame,
        )
        ScannerScreen(
            state = screenState,
            cameraState = cameraState,
            viewfinderFrame = viewfinderFrame,
            onAction = ::handleScreenAction,
        )
    }
}

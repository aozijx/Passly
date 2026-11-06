package com.aozijx.passly.feature.scanner.presentation

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.ui.ScannerScreen
import com.aozijx.passly.feature.scanner.presentation.ui.ScannerScreenModel
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker
import kotlinx.coroutines.launch

@Composable
internal fun ScannerRoute(
    onResult: suspend (String) -> ScannerResultHandling,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ScannerViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val resultSession = remember(state) { ScannerResultSession() }
    val resultPhase by resultSession.phase.collectAsStateWithLifecycle()
    DisposableEffect(resultSession) {
        onDispose { resultSession.close() }
    }
    val context = LocalContext.current
    val copySucceededMessage = stringResource(R.string.scanner_copy_succeeded)
    val rejectedMessage = stringResource(R.string.scanner_result_rejected)
    val imageNoBarcodeMessage = stringResource(R.string.scanner_image_no_barcode)
    val imageDecodeFailedMessage = stringResource(R.string.scanner_image_decode_failed)
    val pickPhoto =
        rememberImagePicker { uri, _ ->
            viewModel.onAction(ScannerUiAction.DecodeImage(ImageRef(uri.toString())))
        }

    fun dismissScanner() {
        resultSession.close()
        viewModel.onAction(ScannerUiAction.StopScanning)
        onDismiss()
    }

    LaunchedEffect(viewModel, lifecycleOwner, context, copySucceededMessage, imageNoBarcodeMessage, imageDecodeFailedMessage) {
        collectScannerRouteEffects(
            lifecycle = lifecycleOwner.lifecycle,
            effects = viewModel.effects,
            onHostStarted = { viewModel.onAction(ScannerUiAction.HostStarted) },
            onHostStopped = { viewModel.onAction(ScannerUiAction.HostStopped) },
            onScanSucceeded = {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
                    .vibrate(
                        VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE),
                    )
            },
            onUiEffect = { effect ->
                val message =
                    when (effect) {
                        ScannerEffect.CopySucceeded -> copySucceededMessage
                        ScannerEffect.ImageNoBarcode -> imageNoBarcodeMessage
                        ScannerEffect.ImageDecodeFailed -> imageDecodeFailedMessage
                        ScannerEffect.ScanSucceeded -> return@collectScannerRouteEffects
                    }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onAction(ScannerUiAction.StopScanning) }
    }

    fun handleScreenAction(action: ScannerScreenAction) {
        val phase = resultSession.phase.value
        if (phase == ScannerResultPhase.Closed ||
            (phase == ScannerResultPhase.Handling && action != ScannerScreenAction.Dismiss)
        ) return

        when (action) {
            ScannerScreenAction.Dismiss -> dismissScanner()
            ScannerScreenAction.PickPhoto -> {
                viewModel.onAction(ScannerUiAction.StartScanning)
                pickPhoto(ImageType.SCREEN)
            }

            ScannerScreenAction.ScanAgain -> viewModel.onAction(ScannerUiAction.StartScanning)
            ScannerScreenAction.CopyResult -> viewModel.onAction(ScannerUiAction.CopyText)
            ScannerScreenAction.ConfirmResult -> {
                val result = (state as? ScannerUiState.ShowingResult)?.result ?: return
                scope.launch {
                    when (resultSession.confirm { onResult(result.rawValue) }) {
                        ScannerResultOutcome.Handled -> dismissScanner()
                        ScannerResultOutcome.Ignored -> Unit
                        ScannerResultOutcome.Rejected -> {
                            Toast.makeText(context, rejectedMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    BackHandler { handleScreenAction(ScannerScreenAction.Dismiss) }

    val screenModel = ScannerScreenModel(
        content = state,
        isHandlingResult = resultPhase == ScannerResultPhase.Handling,
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        val density = LocalDensity.current
        val viewfinderFrame = remember(maxWidth, maxHeight, density) {
            ScannerViewfinderFrame.fitContainer(maxWidth, maxHeight, density)
        }
        ScannerCameraHost(
            onBarcodeDetected = { viewModel.onAction(ScannerUiAction.BarcodeDetected(it)) },
            onPermissionDenied = ::dismissScanner,
            onFailure = { viewModel.onAction(ScannerUiAction.CameraFailure(it)) },
            isScanning = screenModel.isScanning,
            viewfinderFrame = viewfinderFrame,
        )
        ScannerScreen(
            model = screenModel,
            viewfinderFrame = viewfinderFrame,
            onAction = ::handleScreenAction,
        )
    }
}

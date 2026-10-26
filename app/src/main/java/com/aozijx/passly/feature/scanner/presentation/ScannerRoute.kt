package com.aozijx.passly.feature.scanner.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.ui.ScannerScreen
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker
import kotlinx.coroutines.launch

@Composable
internal fun ScannerRoute(
    purpose: ScannerPurpose,
    onImport: suspend (ScannerImport) -> ScannerImportHandlingResult,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ScannerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val importSession = remember(state) { ScannerImportSession() }
    val importPhase by importSession.phase.collectAsStateWithLifecycle()
    DisposableEffect(importSession) {
        onDispose { importSession.close() }
    }
    val context = LocalContext.current
    val copySucceededMessage = stringResource(R.string.scanner_copy_succeeded)
    val openLinkFailedMessage = stringResource(R.string.scanner_open_link_failed)
    val importUnavailableMessage = stringResource(R.string.scanner_import_unavailable)
    val imageNoBarcodeMessage = stringResource(R.string.scanner_image_no_barcode)
    val imageDecodeFailedMessage = stringResource(R.string.scanner_image_decode_failed)
    val pickPhoto = rememberImagePicker { uri, _ ->
        viewModel.onAction(ScannerUiAction.DecodeImage(ImageRef(uri.toString())))
    }

    LaunchedEffect(viewModel, context, copySucceededMessage, imageNoBarcodeMessage, imageDecodeFailedMessage) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                ScannerEffect.CopySucceeded -> copySucceededMessage
                ScannerEffect.ImageNoBarcode -> imageNoBarcodeMessage
                ScannerEffect.ImageDecodeFailed -> imageDecodeFailedMessage
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(viewModel) {
        viewModel.onAction(ScannerUiAction.StartScanning)
        onDispose { viewModel.onAction(ScannerUiAction.StopScanning) }
    }

    ScannerScreen(
        state = state,
        purpose = purpose,
        importing = importPhase == ScannerImportPhase.Importing,
        onAction = { action ->
            if (importSession.phase.value == ScannerImportPhase.Closed) return@ScannerScreen
            if (importSession.phase.value == ScannerImportPhase.Importing &&
                action != ScannerScreenAction.Dismiss && action != ScannerScreenAction.CameraPermissionDenied
            ) return@ScannerScreen
            when (action) {
                ScannerScreenAction.Dismiss,
                ScannerScreenAction.CameraPermissionDenied,
                -> {
                    importSession.close()
                    onDismiss()
                }

                is ScannerScreenAction.CameraFailure ->
                    viewModel.onAction(ScannerUiAction.CameraFailure(action.error))

                ScannerScreenAction.PickPhoto -> {
                    viewModel.onAction(ScannerUiAction.StartScanning)
                    pickPhoto(ImageType.SCREEN)
                }

                ScannerScreenAction.ScanAgain ->
                    viewModel.onAction(ScannerUiAction.StartScanning)

                is ScannerScreenAction.BarcodeDetected ->
                    viewModel.onAction(ScannerUiAction.BarcodeDetected(action.rawValue))

                ScannerScreenAction.ConfirmResult -> {
                    val result = (state as? ScannerUiState.ShowingResult)?.result ?: return@ScannerScreen
                    when (result) {
                        is ScannerResult.Otp,
                        is ScannerResult.Wifi,
                        -> when (val resolution = purpose.resolveImport(result)) {
                            is ScannerImportResolution.Available -> {
                                scope.launch {
                                    when (importSession.confirm { onImport(resolution.value) }) {
                                        ScannerImportOutcome.Applied -> onDismiss()
                                        ScannerImportOutcome.Ignored -> Unit
                                        ScannerImportOutcome.Rejected -> Toast.makeText(
                                            context,
                                            importUnavailableMessage,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }

                            ScannerImportResolution.Unavailable -> Unit
                        }

                        is ScannerResult.WebLink -> try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, result.uri.toString().toUri()),
                            )
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                openLinkFailedMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        is ScannerResult.PlainText ->
                            viewModel.onAction(ScannerUiAction.CopyText)
                    }
                }
            }
        },
    )
}

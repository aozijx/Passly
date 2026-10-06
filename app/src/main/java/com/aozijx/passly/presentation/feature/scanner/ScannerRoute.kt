package com.aozijx.passly.presentation.feature.scanner

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.presentation.feature.scanner.ui.ScannerScreen
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker

@Composable
internal fun ScannerRoute(
    otpConfirmation: ScannerOtpConfirmation,
    onOtpConfirmed: (OtpConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ScannerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copySucceededMessage = stringResource(R.string.scanner_copy_succeeded)
    val openLinkFailedMessage = stringResource(R.string.scanner_open_link_failed)
    val pickPhoto = rememberImagePicker { uri, _ ->
        viewModel.onAction(ScannerUiAction.DecodeImage(ImageRef(uri.toString())))
    }

    LaunchedEffect(viewModel, context, copySucceededMessage) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                ScannerEffect.CopySucceeded -> copySucceededMessage
                is ScannerEffect.ShowError -> effect.message
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
        otpConfirmation = otpConfirmation,
        onAction = { action ->
            when (action) {
                ScannerScreenAction.Dismiss,
                ScannerScreenAction.CameraPermissionDenied,
                -> onDismiss()

                ScannerScreenAction.PickPhoto -> {
                    viewModel.onAction(ScannerUiAction.StartScanning)
                    pickPhoto(ImageType.SCREEN)
                }

                ScannerScreenAction.ScanAgain ->
                    viewModel.onAction(ScannerUiAction.StartScanning)

                is ScannerScreenAction.BarcodeDetected ->
                    viewModel.onAction(ScannerUiAction.BarcodeDetected(action.rawValue))

                ScannerScreenAction.ConfirmResult -> when (val result = state.result) {
                    is ScannerResult.Otp -> {
                        onOtpConfirmed(result.config)
                        onDismiss()
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

                    null -> Unit
                }
            }
        },
    )
}

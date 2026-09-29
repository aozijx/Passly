package com.aozijx.passly.presentation.feature.scanner

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.presentation.feature.scanner.ui.ScannerScreen
import com.aozijx.passly.presentation.shared.media.ImageType
import com.aozijx.passly.presentation.shared.media.rememberImagePicker

@Composable
internal fun ScannerRoute(
    onSaveOtp: (OtpConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ScannerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val unsupportedOtpMessage = stringResource(R.string.vault_scanner_error_not_otp)
    val pickPhoto = rememberImagePicker { uri, _ ->
        viewModel.onAction(ScannerUiAction.DecodeImage(ImageRef(uri.toString())))
    }

    LaunchedEffect(viewModel, context, unsupportedOtpMessage) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                ScannerEffect.UnsupportedOtp -> unsupportedOtpMessage
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
        onAction = viewModel::onAction,
        onPickPhoto = { pickPhoto(ImageType.SCREEN) },
        onSaveOtp = onSaveOtp,
        onDismiss = onDismiss,
    )
}

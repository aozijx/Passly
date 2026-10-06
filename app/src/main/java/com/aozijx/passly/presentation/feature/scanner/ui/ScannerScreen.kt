package com.aozijx.passly.presentation.feature.scanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.presentation.feature.scanner.ScannerCameraHost
import com.aozijx.passly.presentation.feature.scanner.ScannerOtpConfirmation
import com.aozijx.passly.presentation.feature.scanner.ScannerScreenAction
import com.aozijx.passly.presentation.feature.scanner.ScannerUiState

@Composable
internal fun ScannerScreen(
    state: ScannerUiState,
    otpConfirmation: ScannerOtpConfirmation,
    onAction: (ScannerScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val motionScheme = MaterialTheme.motionScheme
    BackHandler { onAction(ScannerScreenAction.Dismiss) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ScannerCameraHost(
            onBarcodeDetected = {
                onAction(ScannerScreenAction.BarcodeDetected(it))
            },
            onPermissionDenied = {
                onAction(ScannerScreenAction.CameraPermissionDenied)
            },
            isScanning = state.isScanning,
        )
        ScannerViewfinder(isScanning = state.isScanning)
        ScannerTopBar(
            onDismiss = { onAction(ScannerScreenAction.Dismiss) },
            onPickPhoto = { onAction(ScannerScreenAction.PickPhoto) },
        )

        AnimatedContent(
            targetState = state.result,
            transitionSpec = {
                (fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                    slideInVertically(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        initialOffsetY = { it / 3 },
                    )).togetherWith(
                    fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                        slideOutVertically(
                            animationSpec = motionScheme.defaultSpatialSpec(),
                            targetOffsetY = { it / 3 },
                        ),
                )
            },
            contentKey = { result -> result?.javaClass },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = if (adaptiveLayout.isAtLeastMedium) 24.dp else 12.dp,
                    vertical = if (adaptiveLayout.isAtLeastMedium) 20.dp else 12.dp,
                )
                .widthIn(max = 560.dp),
            label = "scanner-result",
        ) { result ->
            if (result != null) {
                ScannerResultCard(
                    result = result,
                    otpConfirmation = otpConfirmation,
                    compact = !adaptiveLayout.isAtLeastMedium,
                    onScanAgain = { onAction(ScannerScreenAction.ScanAgain) },
                    onConfirm = { onAction(ScannerScreenAction.ConfirmResult) },
                )
            }
        }
    }
}

@Composable
private fun ScannerTopBar(
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ScannerTopBarButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = stringResource(R.string.scanner_title),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        ScannerTopBarButton(
            icon = Icons.Default.PhotoLibrary,
            contentDescription = stringResource(R.string.scanner_action_album),
            onClick = onPickPhoto,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun ScannerTopBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.42f), CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

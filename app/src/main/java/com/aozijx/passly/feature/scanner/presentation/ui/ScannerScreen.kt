package com.aozijx.passly.feature.scanner.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.feature.scanner.presentation.ScannerScreenAction
import com.aozijx.passly.feature.scanner.presentation.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.ScannerViewfinderFrame

@Composable
internal fun ScannerScreen(
    model: ScannerScreenModel,
    viewfinderFrame: ScannerViewfinderFrame,
    onAction: (ScannerScreenAction) -> Unit,
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val motionScheme = MaterialTheme.motionScheme

    Box(modifier = Modifier.fillMaxSize()) {
        ScannerViewfinder(isScanning = model.isScanning, frame = viewfinderFrame)
        ScannerTopBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            isBusy = model.isHandlingResult,
            onAction = onAction,
        )

        AnimatedContent(
            targetState = model.content,
            transitionSpec = {
                (fadeIn(animationSpec = motionScheme.fastEffectsSpec()) + slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it / 3 },
                )).togetherWith(
                        fadeOut(animationSpec = motionScheme.fastEffectsSpec()) + slideOutVertically(
                            animationSpec = motionScheme.defaultSpatialSpec(),
                            targetOffsetY = { it / 3 },
                        ),
                    ).using(null)
            },
            contentKey = { scannerState -> scannerState::class },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = if (adaptiveLayout.isAtLeastMedium) 24.dp else 12.dp,
                    vertical = if (adaptiveLayout.isAtLeastMedium) 20.dp else 12.dp,
                )
                .widthIn(max = 560.dp),
            label = "scanner-result",
        ) { scannerState ->
            when (scannerState) {
                is ScannerUiState.ShowingResult -> {
                    ScannerResultCard(
                        rawValue = scannerState.result.rawValue,
                        compact = !adaptiveLayout.isAtLeastMedium,
                        isBusy = model.isHandlingResult,
                        onAction = onAction,
                    )
                }

                ScannerUiState.ProcessingImage -> {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = stringResource(R.string.scanner_processing_image),
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                ScannerUiState.Scanning,
                ScannerUiState.Stopped,
                    -> {
                }
            }
        }
    }
}

@Composable
private fun ScannerTopBar(
    modifier: Modifier,
    isBusy: Boolean,
    onAction: (ScannerScreenAction) -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.scanner_title)) },
        navigationIcon = {
            IconButton(onClick = { onAction(ScannerScreenAction.Dismiss) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            IconButton(enabled = !isBusy, onClick = { onAction(ScannerScreenAction.PickPhoto) }) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.scanner_action_album),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

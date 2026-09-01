package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.media.toLocalIconImageModel

@Composable
fun FaviconCropScreen(
    stagedPath: String,
    processing: Boolean,
    onCrop: (zoom: Float, offsetX: Float, offsetY: Float) -> Unit,
    onUseWithoutCrop: () -> Unit,
    onCancel: () -> Unit,
) {
    var cropState by remember(stagedPath) { mutableStateOf(FaviconCropGestureState()) }
    var geometry by remember(stagedPath) { mutableStateOf(FaviconCropGeometry()) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.vault_detail_favicon_crop_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .onSizeChanged { size ->
                            geometry = geometry.copy(
                                viewportWidth = size.width.toFloat(),
                                viewportHeight = size.height.toFloat(),
                            )
                            cropState = FaviconCropTransform.clamp(cropState, geometry)
                        }
                        .pointerInput(stagedPath) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                cropState = FaviconCropTransform.applyGesture(
                                    state = cropState,
                                    geometry = geometry,
                                    panX = pan.x,
                                    panY = pan.y,
                                    zoomChange = gestureZoom,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = toLocalIconImageModel(stagedPath),
                        contentDescription = null,
                        onSuccess = { success ->
                            val drawable = success.result.drawable
                            geometry = geometry.copy(
                                imageWidth = drawable.intrinsicWidth.coerceAtLeast(0).toFloat(),
                                imageHeight = drawable.intrinsicHeight.coerceAtLeast(0).toFloat(),
                            )
                            cropState = FaviconCropTransform.clamp(cropState, geometry)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = cropState.zoom
                                scaleY = cropState.zoom
                                translationX = cropState.offsetX
                                translationY = cropState.offsetY
                                clip = true
                            },
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    text = stringResource(R.string.vault_detail_favicon_crop_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onCancel, enabled = !processing) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = onUseWithoutCrop,
                        enabled = !processing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.vault_detail_favicon_no_crop))
                    }
                    Button(
                        onClick = {
                            val selection = FaviconCropTransform.toRequest(cropState, geometry)
                            onCrop(
                                selection.zoom,
                                selection.offsetX,
                                selection.offsetY,
                            )
                        },
                        enabled = !processing,
                    ) {
                        Text(stringResource(R.string.vault_detail_favicon_crop_use))
                    }
                }
            }
        }
    }
}

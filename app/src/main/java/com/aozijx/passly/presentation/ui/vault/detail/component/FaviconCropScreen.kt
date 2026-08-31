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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
    var zoom by remember(stagedPath) { mutableFloatStateOf(1f) }
    var offset by remember(stagedPath) { mutableStateOf(Offset.Zero) }
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
                        .pointerInput(stagedPath) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = toLocalIconImageModel(stagedPath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offset.x
                                translationY = offset.y
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
                            onCrop(
                                zoom,
                                (offset.x / 300f).coerceIn(-1f, 1f),
                                (offset.y / 300f).coerceIn(-1f, 1f),
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

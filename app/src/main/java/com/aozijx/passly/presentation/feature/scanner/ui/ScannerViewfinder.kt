package com.aozijx.passly.presentation.feature.scanner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
internal fun ScannerViewfinder(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val frameSize = minOf(maxWidth * 0.72f, maxHeight * 0.48f, 340.dp)
        val density = LocalDensity.current
        val frameSizePx = with(density) { frameSize.toPx() }
        val cornerRadiusPx = with(density) { 28.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            val topLeft = Offset(
                x = (size.width - frameSizePx) / 2f,
                y = (size.height - frameSizePx) / 2f,
            )
            drawRect(Color.Black.copy(alpha = 0.56f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = topLeft,
                size = Size(frameSizePx, frameSizePx),
                cornerRadius = CornerRadius(cornerRadiusPx),
                blendMode = BlendMode.Clear,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(frameSize)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(28.dp)
                },
        ) {
            ScannerFrame(modifier = Modifier.fillMaxSize())
            if (isScanning) {
                val transition = rememberInfiniteTransition(label = "scanner-line")
                val progress by transition.animateFloat(
                    initialValue = 0.08f,
                    targetValue = 0.92f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1_600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "scanner-line-progress",
                )
                val travelPx = with(density) { (frameSize - 2.dp).toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .graphicsLayer { translationY = travelPx * progress }
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.5f to MaterialTheme.colorScheme.primary,
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = frameSize / 2 + 32.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.58f),
            contentColor = Color.White,
        ) {
            Text(
                text = stringResource(R.string.scanner_supported_formats),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val outlineWidth = 1.5.dp.toPx()
        val cornerWidth = 4.dp.toPx()
        val inset = cornerWidth / 2f
        val cornerLength = 34.dp.toPx()
        val maxX = size.width - inset
        val maxY = size.height - inset

        drawRoundRect(
            color = Color.White.copy(alpha = 0.42f),
            cornerRadius = CornerRadius(28.dp.toPx()),
            style = Stroke(width = outlineWidth),
        )

        fun corner(start: Offset, horizontal: Offset, vertical: Offset) {
            drawLine(primary, start, horizontal, cornerWidth, StrokeCap.Round)
            drawLine(primary, start, vertical, cornerWidth, StrokeCap.Round)
        }

        corner(
            start = Offset(inset, inset),
            horizontal = Offset(inset + cornerLength, inset),
            vertical = Offset(inset, inset + cornerLength),
        )
        corner(
            start = Offset(maxX, inset),
            horizontal = Offset(maxX - cornerLength, inset),
            vertical = Offset(maxX, inset + cornerLength),
        )
        corner(
            start = Offset(inset, maxY),
            horizontal = Offset(inset + cornerLength, maxY),
            vertical = Offset(inset, maxY - cornerLength),
        )
        corner(
            start = Offset(maxX, maxY),
            horizontal = Offset(maxX - cornerLength, maxY),
            vertical = Offset(maxX, maxY - cornerLength),
        )
    }
}

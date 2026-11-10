package com.aozijx.passly.feature.scanner.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.scanner.presentation.state.ScannerCameraState
import com.aozijx.passly.feature.scanner.presentation.viewfinder.ScannerViewfinderFrame

private val VIEWFINDER_CORNER_RADIUS = 28.dp
private val FRAME_DECORATION_LENGTH = 42.dp
private val FRAME_OUTLINE_WIDTH = 1.5.dp
private val FRAME_DECORATION_WIDTH = 4.dp

@Composable
internal fun ScannerViewfinder(
    cameraState: ScannerCameraState,
    frame: ScannerViewfinderFrame,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val frameSize = frame.size
        val density = LocalDensity.current
        val frameSizePx = frame.sizePx
        val cornerRadiusPx = with(density) { VIEWFINDER_CORNER_RADIUS.toPx() }

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            val topLeft =
                Offset(
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
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(frameSize),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(VIEWFINDER_CORNER_RADIUS)
                        },
            ) {
                when (cameraState) {
                    ScannerCameraState.Active -> {
                        val transition = rememberInfiniteTransition(label = "scanner-line")
                        val progress by transition.animateFloat(
                            initialValue = 0.08f,
                            targetValue = 0.92f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(durationMillis = 1_600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            label = "scanner-line-progress",
                        )
                        val travelPx = with(density) { (frameSize - 2.dp).toPx() }
                        Box(
                            modifier =
                                Modifier
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

                    ScannerCameraState.Idle -> Unit
                }
            }
            // Draw the frame after the clipped scan content so its corner marks stay visible.
            ScannerFrame(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val outlineWidth = FRAME_OUTLINE_WIDTH.toPx()
        val cornerWidth = FRAME_DECORATION_WIDTH.toPx()
        val inset = cornerWidth / 2f
        val cornerRadius = VIEWFINDER_CORNER_RADIUS.toPx() - inset
        val cornerLength = FRAME_DECORATION_LENGTH.toPx()
        val maxX = size.width - inset
        val maxY = size.height - inset

        drawRoundRect(
            color = Color.White.copy(alpha = 0.42f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = outlineWidth),
        )

        fun corner(
            arcTopLeft: Offset,
            startAngle: Float,
            horizontalStart: Offset,
            horizontalEnd: Offset,
            verticalStart: Offset,
            verticalEnd: Offset,
        ) {
            drawArc(
                color = primary,
                startAngle = startAngle,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = Size(cornerRadius * 2f, cornerRadius * 2f),
                style = Stroke(width = cornerWidth, cap = StrokeCap.Round),
            )
            drawLine(primary, horizontalStart, horizontalEnd, cornerWidth, StrokeCap.Round)
            drawLine(primary, verticalStart, verticalEnd, cornerWidth, StrokeCap.Round)
        }

        corner(
            arcTopLeft = Offset(inset, inset),
            startAngle = 180f,
            horizontalStart = Offset(inset + cornerRadius, inset),
            horizontalEnd = Offset(inset + cornerLength, inset),
            verticalStart = Offset(inset, inset + cornerRadius),
            verticalEnd = Offset(inset, inset + cornerLength),
        )
        corner(
            arcTopLeft = Offset(maxX - cornerRadius * 2f, inset),
            startAngle = 270f,
            horizontalStart = Offset(maxX - cornerLength, inset),
            horizontalEnd = Offset(maxX - cornerRadius, inset),
            verticalStart = Offset(maxX, inset + cornerRadius),
            verticalEnd = Offset(maxX, inset + cornerLength),
        )
        corner(
            arcTopLeft = Offset(inset, maxY - cornerRadius * 2f),
            startAngle = 90f,
            horizontalStart = Offset(inset + cornerRadius, maxY),
            horizontalEnd = Offset(inset + cornerLength, maxY),
            verticalStart = Offset(inset, maxY - cornerLength),
            verticalEnd = Offset(inset, maxY - cornerRadius),
        )
        corner(
            arcTopLeft = Offset(maxX - cornerRadius * 2f, maxY - cornerRadius * 2f),
            startAngle = 0f,
            horizontalStart = Offset(maxX - cornerLength, maxY),
            horizontalEnd = Offset(maxX - cornerRadius, maxY),
            verticalStart = Offset(maxX, maxY - cornerLength),
            verticalEnd = Offset(maxX, maxY - cornerRadius),
        )
    }
}

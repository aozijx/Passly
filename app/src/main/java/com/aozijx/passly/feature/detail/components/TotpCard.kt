package com.aozijx.passly.feature.detail.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.shape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.feature.vault.model.OtpUiState
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpCard(
    currentState: OtpUiState?,
    totpUri: String? = null,
    qrBitmap: Bitmap? = null,
    showProgress: Boolean = true,
    onQrClick: (() -> Unit)? = null,
    onCodeClick: (() -> Unit)? = null
) {
    var showQrSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "一次性密码",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            IconButton(
                onClick = {
                    showQrSheet = true
                    onQrClick?.invoke()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.QrCode,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(onClick = { onCodeClick?.invoke() }),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.2f
                )
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            // 逻辑预处理
            val code = currentState?.code
            val progress = currentState?.progress ?: 0f

            val displayText = remember(code) {
                code?.chunked(3)?.joinToString(" ") ?: "------"
            }

            // 状态平滑动画 (可选优化)
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                label = "otp_progress"
            )

            // 样式解耦
            val codeStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showProgress) {
                    TotpWaveProgressIndicator(
                        progress = animatedProgress,
                        isExpiring = progress < 0.2f,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    text = displayText,
                    style = codeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (showQrSheet) {
            QRcodeRender(
                totpUri = totpUri,
                externalBitmap = qrBitmap,
                sheetState = sheetState,
                onDismiss = { showQrSheet = false }
            )
        }
    }
}

@Composable
private fun TotpWaveProgressIndicator(
    progress: Float,
    isExpiring: Boolean,
    modifier: Modifier = Modifier
) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val waveColor = if (isExpiring) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val ringColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val circle = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(topLeft, Size(diameter, diameter)))
        }
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(containerColor, radius = radius, center = center)
        clipPath(circle) {
            val waveHeight = diameter * 0.10f
            val fillTop = topLeft.y + diameter * (1f - boundedProgress)
            val path = Path().apply {
                moveTo(topLeft.x, fillTop)
                val steps = 32
                for (step in 0..steps) {
                    val x = topLeft.x + diameter * step / steps
                    val phase = step / steps.toFloat() * PI * 2.0
                    val y = fillTop + sin(phase).toFloat() * waveHeight
                    lineTo(x, y)
                }
                lineTo(topLeft.x + diameter, topLeft.y + diameter)
                lineTo(topLeft.x, topLeft.y + diameter)
                close()
            }
            drawPath(path, waveColor.copy(alpha = 0.86f))
        }
        drawCircle(
            color = ringColor,
            radius = radius - 1.dp.toPx(),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QRcodeRender(
    totpUri: String?,
    externalBitmap: Bitmap?,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val bitmap = remember(totpUri, externalBitmap) {
        externalBitmap ?: totpUri?.let { QrCodeUtils.generateQrCode(it, 512) }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.vault_export_qr_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            Text(
                text = stringResource(R.string.vault_export_qr_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 错误状态 UI
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCode,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text("无法生成二维码", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

            }
        }
    }
}

package com.aozijx.passly.presentation.ui.vault.detail.component

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.app.qr.QrCodeEncoder
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpCard(
    currentState: DetailOtpUiModel?,
    totpUri: String? = null,
    qrBitmap: Bitmap? = null,
    showProgress: Boolean = true,
    onQrClick: (() -> Unit)? = null,
    onCodeClick: (() -> Unit)? = null
) {
    var showQrSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.vault_detail_totp_label),
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
            onClick = { onCodeClick?.invoke() },
            modifier = Modifier.fillMaxWidth(),
            enabled = onCodeClick != null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.2f
                )
            ),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            val code = currentState?.code
            val progress = currentState?.progress ?: 0f

            val displayText = remember(code) {
                code?.chunked(3)?.joinToString(" ") ?: "------"
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                label = "otp_progress"
            )

            val codeStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showProgress) {
                    TotpCircularWavyProgressIndicator(
                        progress = animatedProgress,
                        isExpiring = progress < 0.2f,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = displayText,
                    style = codeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.wrapContentWidth()
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
private fun TotpCircularWavyProgressIndicator(
    progress: Float,
    isExpiring: Boolean,
    modifier: Modifier = Modifier
) {
    val progressColor = if (isExpiring) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    CircularWavyProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
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
        externalBitmap ?: totpUri?.let { QrCodeEncoder.encode(it, 512) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
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
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.largeIncreased,
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

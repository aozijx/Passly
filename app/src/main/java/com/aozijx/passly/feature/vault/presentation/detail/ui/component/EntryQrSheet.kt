package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.qr.QrCodeEncoder
import com.aozijx.passly.feature.vault.detail.EntryQrKind
import com.aozijx.passly.feature.vault.presentation.detail.DetailQrState
import com.aozijx.passly.feature.vault.presentation.detail.DetailQrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryQrSheet(state: DetailQrState, onDismiss: () -> Unit) {
    val kind = when (state) {
        DetailQrState.Hidden -> return
        is DetailQrState.Loading -> state.kind
        is DetailQrState.Ready -> state.kind
        is DetailQrState.Failed -> state.kind
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(when (kind) {
                    EntryQrKind.Otp -> R.string.vault_export_qr_title
                    EntryQrKind.Wifi -> R.string.vault_wifi_qr_title
                }),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(when (kind) {
                    EntryQrKind.Otp -> R.string.vault_export_qr_message
                    EntryQrKind.Wifi -> R.string.vault_wifi_qr_message
                }),
                style = MaterialTheme.typography.bodyMedium,
            )
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 360.dp)
                    .fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        DetailQrState.Hidden -> Unit
                        is DetailQrState.Loading -> CircularWavyProgressIndicator()
                        is DetailQrState.Failed -> Text(stringResource(R.string.vault_qr_generation_failed))
                        is DetailQrState.Ready -> EntryQrImage(state.content)
                    }
                }
            }
        }
    }
}

private sealed interface RenderedQr {
    data object Loading : RenderedQr
    data class Ready(val bitmap: Bitmap) : RenderedQr
    data object Failed : RenderedQr
}

@Composable
private fun EntryQrImage(content: DetailQrPayload) {
    val rendered by produceState<RenderedQr>(RenderedQr.Loading, content) {
        var owned: Bitmap? = null
        var cleanup: AutoCloseable? = null
        try {
            withContext(Dispatchers.Default) {
                owned = content.useChars { chars ->
                    if (chars.isEmpty()) null else QrCodeEncoder.encode(String(chars))
                }
            }
            owned?.let { bitmap ->
                cleanup = content.onClose {
                    if (!bitmap.isRecycled) bitmap.eraseColor(Color.WHITE)
                }
            }
            value = owned?.let(RenderedQr::Ready) ?: RenderedQr.Failed
            awaitCancellation()
        } finally {
            cleanup?.close()
            owned?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.eraseColor(Color.WHITE)
                    bitmap.recycle()
                }
            }
        }
    }
    when (val current = rendered) {
        RenderedQr.Loading -> CircularWavyProgressIndicator()
        is RenderedQr.Ready -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.vault_qr_image),
            modifier = Modifier.fillMaxSize(),
        )
        RenderedQr.Failed -> Text(stringResource(R.string.vault_qr_generation_failed))
    }
}

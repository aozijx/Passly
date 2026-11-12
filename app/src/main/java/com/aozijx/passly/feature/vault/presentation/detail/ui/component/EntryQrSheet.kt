package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.qr.QrCodeMatrix
import com.aozijx.passly.core.ui.components.passlySheetTitleStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailQrState
import com.aozijx.passly.feature.vault.presentation.detail.EntryQrKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryQrSheet(
    state: DetailQrState,
    onDismiss: () -> Unit,
) {
    val kind =
        when (state) {
            DetailQrState.Hidden -> return
            is DetailQrState.Authorizing -> return
            is DetailQrState.Loading -> state.kind
            is DetailQrState.Ready -> state.kind
            is DetailQrState.Failed -> state.kind
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(
                    when (kind) {
                        EntryQrKind.Otp -> R.string.vault_export_qr_title
                        EntryQrKind.Wifi -> R.string.vault_wifi_qr_title
                    },
                ),
                style = passlySheetTitleStyle(),
            )
            Text(
                stringResource(
                    when (kind) {
                        EntryQrKind.Otp -> R.string.vault_export_qr_message
                        EntryQrKind.Wifi -> R.string.vault_wifi_qr_message
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
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
                        is DetailQrState.Authorizing -> Unit
                        is DetailQrState.Loading -> CircularWavyProgressIndicator()
                        is DetailQrState.Failed -> Text(stringResource(R.string.vault_qr_generation_failed))
                        is DetailQrState.Ready -> EntryQrImage(state.matrix)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryQrImage(matrix: QrCodeMatrix) {
    val description = stringResource(R.string.vault_qr_image)
    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = description },
    ) {
        drawRect(Color.White)
        val moduleWidth = size.width / matrix.width
        val moduleHeight = size.height / matrix.height
        val path = Path()
        for (row in 0 until matrix.height) {
            for (column in 0 until matrix.width) {
                if (matrix.isDark(column, row)) {
                    val left = column * moduleWidth
                    val top = row * moduleHeight
                    path.addRect(
                        Rect(
                            left = left,
                            top = top,
                            right = left + moduleWidth,
                            bottom = top + moduleHeight,
                        ),
                    )
                }
            }
        }
        drawPath(path, Color.Black)
    }
}

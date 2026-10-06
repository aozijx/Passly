package com.aozijx.passly.presentation.feature.scanner.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.presentation.feature.scanner.ScannerOtpConfirmation
import com.aozijx.passly.presentation.feature.scanner.ScannerResult

@Composable
internal fun ScannerResultCard(
    result: ScannerResult,
    otpConfirmation: ScannerOtpConfirmation,
    compact: Boolean,
    onScanAgain: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackOtpLabel = stringResource(R.string.scanner_otp_fallback_label)
    val presentation = when (result) {
        is ScannerResult.Otp -> ScannerResultPresentation(
            icon = Icons.Default.Key,
            title = stringResource(R.string.scanner_result_otp),
            detail = result.config.displayLabel(fallbackOtpLabel),
            preview = null,
            primaryLabel = stringResource(
                when (otpConfirmation) {
                    ScannerOtpConfirmation.ADD_TO_VAULT -> R.string.scanner_add_to_vault
                    ScannerOtpConfirmation.APPLY_TO_EDITOR -> R.string.scanner_apply_to_editor
                },
            ),
        )

        is ScannerResult.WebLink -> ScannerResultPresentation(
            icon = Icons.Default.Link,
            title = stringResource(R.string.scanner_result_link),
            detail = result.uri.host,
            preview = result.rawValue,
            primaryLabel = stringResource(R.string.scanner_open_link),
        )

        is ScannerResult.PlainText -> ScannerResultPresentation(
            icon = Icons.Default.ContentCopy,
            title = stringResource(R.string.scanner_result_text),
            detail = result.rawValue,
            preview = null,
            primaryLabel = stringResource(R.string.scanner_copy_text),
        )
    }
    val motionScheme = MaterialTheme.motionScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = motionScheme.defaultSpatialSpec()),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 16.dp else 24.dp,
                vertical = if (compact) 16.dp else 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = presentation.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (result is ScannerResult.PlainText) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            presentation.preview?.let { preview ->
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.scanner_scan_again))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.35f),
                ) {
                    Text(presentation.primaryLabel)
                }
            }
        }
    }
}

private data class ScannerResultPresentation(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val preview: String?,
    val primaryLabel: String,
)

private fun OtpConfig.displayLabel(fallback: String): String = buildString {
    if (!issuer.isNullOrBlank()) append(issuer)
    if (!accountName.isNullOrBlank()) {
        if (isNotEmpty()) append(": ")
        append(accountName)
    }
    if (isEmpty()) append(fallback)
}

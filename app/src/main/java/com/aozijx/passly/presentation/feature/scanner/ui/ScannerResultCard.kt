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
import androidx.compose.material.icons.filled.Wifi
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
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.presentation.feature.scanner.ScannerPurpose
import com.aozijx.passly.presentation.feature.scanner.ScannerResult

@Composable
internal fun ScannerResultCard(
    result: ScannerResult,
    purpose: ScannerPurpose,
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
            primaryAction = when (purpose) {
                ScannerPurpose.General -> ScannerPrimaryActionPresentation.Available(
                    stringResource(R.string.scanner_add_to_vault),
                )
                ScannerPurpose.ApplyToOtpEditor -> ScannerPrimaryActionPresentation.Available(
                    stringResource(R.string.scanner_apply_to_editor),
                )
                ScannerPurpose.ApplyToWifiEditor -> ScannerPrimaryActionPresentation.Unavailable
            },
        )

        is ScannerResult.Wifi -> ScannerResultPresentation(
            icon = Icons.Default.Wifi,
            title = stringResource(R.string.scanner_result_wifi),
            detail = result.payload.ssid,
            preview = buildList {
                add(result.payload.securityType.displayName())
                if (result.payload.isHidden) add(stringResource(R.string.wifi_hidden))
            }.joinToString(" · "),
            primaryAction = when (purpose) {
                ScannerPurpose.General -> ScannerPrimaryActionPresentation.Available(
                    stringResource(R.string.scanner_edit_wifi),
                )
                ScannerPurpose.ApplyToWifiEditor -> ScannerPrimaryActionPresentation.Available(
                    stringResource(R.string.scanner_apply_to_editor),
                )
                ScannerPurpose.ApplyToOtpEditor -> ScannerPrimaryActionPresentation.Unavailable
            },
        )

        is ScannerResult.WebLink -> ScannerResultPresentation(
            icon = Icons.Default.Link,
            title = stringResource(R.string.scanner_result_link),
            detail = result.uri.host,
            preview = result.rawValue,
            primaryAction = ScannerPrimaryActionPresentation.Available(
                stringResource(R.string.scanner_open_link),
            ),
        )

        is ScannerResult.PlainText -> ScannerResultPresentation(
            icon = Icons.Default.ContentCopy,
            title = stringResource(R.string.scanner_result_text),
            detail = result.rawValue,
            preview = null,
            primaryAction = ScannerPrimaryActionPresentation.Available(
                stringResource(R.string.scanner_copy_text),
            ),
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
                when (val action = presentation.primaryAction) {
                    is ScannerPrimaryActionPresentation.Available -> Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.35f),
                    ) {
                        Text(action.label)
                    }
                    ScannerPrimaryActionPresentation.Unavailable -> Unit
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
    val primaryAction: ScannerPrimaryActionPresentation,
)

private sealed interface ScannerPrimaryActionPresentation {
    data class Available(val label: String) : ScannerPrimaryActionPresentation
    data object Unavailable : ScannerPrimaryActionPresentation
}

@Composable
private fun WifiSecurityType.displayName(): String = stringResource(
    when (this) {
        WifiSecurityType.Open -> R.string.wifi_security_open
        WifiSecurityType.Wep -> R.string.wifi_security_wep
        WifiSecurityType.WpaPersonal -> R.string.wifi_security_wpa_personal
    },
)

private fun OtpConfig.displayLabel(fallback: String): String = buildString {
    if (!issuer.isNullOrBlank()) append(issuer)
    if (!accountName.isNullOrBlank()) {
        if (isNotEmpty()) append(": ")
        append(accountName)
    }
    if (isEmpty()) append(fallback)
}

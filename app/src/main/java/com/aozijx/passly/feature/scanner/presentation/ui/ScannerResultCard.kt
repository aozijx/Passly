package com.aozijx.passly.feature.scanner.presentation.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.scanner.presentation.action.ScannerScreenAction
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerPrimaryActionLabel
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultTextInteraction
import com.aozijx.passly.feature.scanner.presentation.state.ScannerResultActionState

@Composable
internal fun ScannerResultCard(
    rawValue: String,
    actions: ScannerResultActionState,
    presentation: ScannerResultPresentation,
    onAction: (ScannerScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val primaryActionLabel =
        when (presentation.primaryActionLabel) {
            ScannerPrimaryActionLabel.Continue -> stringResource(R.string.scanner_continue)
            ScannerPrimaryActionLabel.Save -> stringResource(R.string.save)
            ScannerPrimaryActionLabel.Use -> stringResource(R.string.scanner_apply_to_editor)
        }
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(animationSpec = motionScheme.defaultSpatialSpec()),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.scanner_result),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = rawValue,
                        modifier =
                            Modifier.clickable(
                                enabled =
                                    presentation.resultTextInteraction is ScannerResultTextInteraction.CopyOnTap &&
                                        actions is ScannerResultActionState.Available,
                                onClickLabel = stringResource(R.string.scanner_copy_text),
                                role = Role.Button,
                                onClick = { onAction(ScannerScreenAction.CopyResult) },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    enabled = actions is ScannerResultActionState.Available,
                    onClick = { onAction(ScannerScreenAction.ScanAgain) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.scanner_scan_again))
                }
                Button(
                    enabled = actions is ScannerResultActionState.Available,
                    onClick = { onAction(ScannerScreenAction.ConfirmResult) },
                    modifier = Modifier.weight(1f),
                ) {
                    when (actions) {
                        ScannerResultActionState.Handling -> {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }

                        ScannerResultActionState.Available,
                        ScannerResultActionState.DismissOnly,
                        ScannerResultActionState.Locked,
                        -> {
                            Unit
                        }
                    }
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

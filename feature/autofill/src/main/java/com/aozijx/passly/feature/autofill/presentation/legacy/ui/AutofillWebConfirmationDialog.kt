package com.aozijx.passly.feature.autofill.presentation.legacy.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.feature.autofill.R

@Composable
internal fun AutofillWebConfirmationDialog(
    packageName: String,
    webDomain: String,
    candidate: AutofillCandidateSummary? = null,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val appLabel = remember(context, packageName) { resolveApplicationLabel(context.packageManager, packageName) }
    val isEntryConfirmation = candidate != null

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(
                    if (isEntryConfirmation) {
                        R.string.autofill_web_entry_confirmation_title
                    } else {
                        R.string.autofill_web_confirmation_title
                    },
                ),
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.autofill_web_app_label, appLabel))
                Text(
                    stringResource(R.string.autofill_web_package_label, packageName),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    stringResource(R.string.autofill_web_domain_label, webDomain),
                    modifier = Modifier.padding(top = 8.dp),
                )
                candidate?.let {
                    Text(
                        stringResource(
                            R.string.autofill_web_entry_label,
                            it.entry.title,
                            it.entry.username,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    stringResource(
                        if (isEntryConfirmation) {
                            R.string.autofill_web_entry_risk
                        } else {
                            R.string.autofill_web_origin_risk
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.autofill_web_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.autofill_cancel))
            }
        },
    )
}

private fun resolveApplicationLabel(
    packageManager: PackageManager,
    packageName: String,
): String =
    try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        packageName
    }

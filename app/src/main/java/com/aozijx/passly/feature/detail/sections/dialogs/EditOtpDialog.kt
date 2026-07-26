package com.aozijx.passly.feature.detail.sections.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.components.OtpConfigForm
import com.aozijx.passly.feature.vault.model.OtpFormState

@Composable
fun EditOtpSection(
    formState: OtpFormState,
    onFieldUpdate: (OtpFormState) -> Unit,
    onTypeChange: (OtpType) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            stringResource(R.string.vault_edit_otp_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        OtpConfigForm(
            state = formState,
            onFieldUpdate = onFieldUpdate,
            onTypeChange = onTypeChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = onSave,
                enabled = formState.isValid
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

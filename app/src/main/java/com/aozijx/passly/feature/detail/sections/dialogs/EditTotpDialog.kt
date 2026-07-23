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
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpType
import com.aozijx.passly.feature.detail.internal.TotpEditState
import com.aozijx.passly.feature.vault.components.TotpConfigForm

@Composable
fun EditTotpSection(
    item: VaultEntry,
    editState: TotpEditState,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.vault_edit_totp_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        TotpConfigForm(
            type = editState.type,
            onTypeChange = editState::selectType,
            secret = editState.secret, onSecretChange = { editState.secret = it },
            encoding = editState.encoding,
            onEncodingChange = { editState.encoding = it },
            period = editState.period, onPeriodChange = { editState.period = it },
            counter = editState.counter, onCounterChange = { editState.counter = it },
            digits = editState.digits, onDigitsChange = { editState.digits = it },
            algorithm = editState.algorithm, onAlgorithmChange = { editState.algorithm = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { editState.isEditing = false }) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = {
                if (editState.secret.isNotBlank()) {
                    try {
                        onEntryUpdated(
                            item.copy(
                                credential = item.credential.copy(
                                    otp = OtpConfig(
                                        type = editState.type,
                                        secret = editState.secret.trim(),
                                        digits = if (editState.type == OtpType.STEAM) 5
                                        else (editState.digits.toIntOrNull() ?: 6),
                                        periodSeconds = if (editState.type == OtpType.HOTP) null
                                        else (editState.period.toIntOrNull() ?: 30),
                                        counter = if (editState.type == OtpType.HOTP) {
                                            editState.counter.toLongOrNull() ?: 0L
                                        } else null,
                                        algorithm = run {
                                            val a = editState.algorithm.uppercase()
                                            when (a) {
                                                "SHA256" -> OtpHashAlgorithm.SHA256
                                                "SHA512" -> OtpHashAlgorithm.SHA512
                                                else -> OtpHashAlgorithm.SHA1
                                            }
                                        },
                                        encoding = editState.encoding,
                                        issuer = item.credential.otp?.issuer,
                                        accountName = item.credential.otp?.accountName
                                    )
                                )
                            )
                        )
                        editState.isEditing = false
                    } catch (e: Exception) {
                    }
                }
            }) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

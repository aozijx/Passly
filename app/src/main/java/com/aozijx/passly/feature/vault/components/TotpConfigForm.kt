package com.aozijx.passly.feature.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.core.OtpSecretEncoding
import com.aozijx.passly.domain.model.core.OtpType

@Composable
fun TotpConfigForm(
    type: OtpType,
    onTypeChange: (OtpType) -> Unit,
    secret: String,
    onSecretChange: (String) -> Unit,
    encoding: OtpSecretEncoding,
    onEncodingChange: (OtpSecretEncoding) -> Unit,
    period: String,
    onPeriodChange: (String) -> Unit,
    counter: String,
    onCounterChange: (String) -> Unit,
    digits: String,
    onDigitsChange: (String) -> Unit,
    algorithm: String,
    onAlgorithmChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.otp_type))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OtpType.entries.forEach { candidate ->
                FilterChip(
                    selected = type == candidate,
                    onClick = { onTypeChange(candidate) },
                    label = { Text(candidate.name) }
                )
            }
        }

        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text(stringResource(R.string.totp_secret)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(stringResource(R.string.otp_secret_encoding))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OtpSecretEncoding.entries.forEach { candidate ->
                FilterChip(
                    selected = encoding == candidate,
                    onClick = { onEncodingChange(candidate) },
                    label = { Text(candidate.name) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (type == OtpType.HOTP) {
                OutlinedTextField(
                    value = counter,
                    onValueChange = onCounterChange,
                    label = { Text(stringResource(R.string.otp_counter)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = period,
                    onValueChange = onPeriodChange,
                    label = { Text(stringResource(R.string.totp_period)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = digits,
                onValueChange = onDigitsChange,
                label = { Text(stringResource(R.string.totp_digits)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = type != OtpType.STEAM,
                singleLine = true
            )
            OutlinedTextField(
                value = algorithm,
                onValueChange = onAlgorithmChange,
                label = { Text(stringResource(R.string.totp_algorithm)) },
                modifier = Modifier.weight(1f),
                enabled = type != OtpType.STEAM,
                singleLine = true
            )
        }
    }
}

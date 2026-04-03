package com.aozijx.passly.core.designsystem.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
fun TotpConfigForm(
    secret: String,
    onSecretChange: (String) -> Unit,
    period: String,
    onPeriodChange: (String) -> Unit,
    digits: String,
    onDigitsChange: (String) -> Unit,
    algorithm: String,
    onAlgorithmChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // TOTP 密钥输入框
        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text(stringResource(R.string.label_totp_secret)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 更新周期 (通常为 30s)
            OutlinedTextField(
                value = period,
                onValueChange = onPeriodChange,
                label = { Text(stringResource(R.string.label_totp_period)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            // 验证码位数 (通常为 6位)
            OutlinedTextField(
                value = digits,
                onValueChange = onDigitsChange,
                label = { Text(stringResource(R.string.label_totp_digits)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            // 加密算法 (如 SHA1, SHA256)
            OutlinedTextField(
                value = algorithm,
                onValueChange = onAlgorithmChange,
                label = { Text(stringResource(R.string.label_totp_algorithm)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}



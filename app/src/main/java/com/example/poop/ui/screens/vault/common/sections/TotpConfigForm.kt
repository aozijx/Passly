package com.example.poop.ui.screens.vault.common.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text("TOTP Secret (Base32)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = period,
                onValueChange = onPeriodChange,
                label = { Text("周期/s") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = digits,
                onValueChange = onDigitsChange,
                label = { Text("位数") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = algorithm,
                onValueChange = onAlgorithmChange,
                label = { Text("算法") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

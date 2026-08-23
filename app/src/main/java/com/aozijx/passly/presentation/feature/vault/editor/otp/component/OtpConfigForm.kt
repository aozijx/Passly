package com.aozijx.passly.presentation.feature.vault.editor.otp.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.common.DropdownSelector
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.presentation.feature.vault.editor.otp.OtpFormState
import com.aozijx.passly.presentation.feature.vault.editor.common.EntryEditorTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpConfigForm(
    modifier: Modifier = Modifier,
    state: OtpFormState,
    onFieldUpdate: (OtpFormState) -> Unit = {},
    onTypeChange: (OtpType) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DropdownSelector(
            value = state.type,
            onValueChange = { onTypeChange(it) },
            options = OtpType.entries,
            label = stringResource(R.string.otp_type),
            optionToString = { it.name }
        )

        EntryEditorTextField(
            value = state.secret,
            onValueChange = { onFieldUpdate(state.copy(secret = it)) },
            label = stringResource(R.string.totp_secret),
            modifier = Modifier.fillMaxWidth()
        )

        DropdownSelector(
            value = OtpHashAlgorithm.entries.find { it.name == state.algorithm }
                ?: OtpHashAlgorithm.SHA1,
            onValueChange = { onFieldUpdate(state.copy(algorithm = it.name)) },
            options = OtpHashAlgorithm.entries,
            label = stringResource(R.string.totp_algorithm),
            enabled = state.type != OtpType.STEAM,
            optionToString = { it.name }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.type == OtpType.HOTP) {
                EntryEditorTextField(
                    value = state.counter,
                    onValueChange = { onFieldUpdate(state.copy(counter = it)) },
                    label = stringResource(R.string.otp_counter),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                EntryEditorTextField(
                    value = state.period,
                    onValueChange = { onFieldUpdate(state.copy(period = it)) },
                    label = stringResource(R.string.totp_period),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            EntryEditorTextField(
                value = state.digits,
                onValueChange = { onFieldUpdate(state.copy(digits = it)) },
                label = stringResource(R.string.totp_digits),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = state.type != OtpType.STEAM
            )
        }

        DropdownSelector(
            value = state.encoding,
            onValueChange = { onFieldUpdate(state.copy(encoding = it)) },
            options = OtpSecretEncoding.entries,
            label = stringResource(R.string.otp_secret_encoding),
            modifier = Modifier.fillMaxWidth(),
            optionToString = { it.name }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TotpConfigFormPreview() {
    val state = remember {
        OtpFormState(
            secret = "JBSWY3DPEHPK3PXP"
        )
    }

    Surface {
        Column(modifier = Modifier.padding(16.dp)) {
            OtpConfigForm(
                state = state
            )
        }
    }
}

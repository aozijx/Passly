package com.aozijx.passly.presentation.ui.vault.editor.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.PasslyOutlinedTextField
import com.aozijx.passly.presentation.ui.shared.components.common.DropdownSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpConfigForm(
    state: OtpEditorState,
    onEvent: OtpEditorEventHandler,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DropdownSelector(
            value = state.type,
            onValueChange = onEvent.onTypeChange,
            options = OtpEditorType.entries,
            label = stringResource(R.string.otp_type),
            optionToString = { it.name },
        )
        PasslyOutlinedTextField(
            value = state.secret,
            onValueChange = onEvent.onSecretChange,
            label = stringResource(R.string.totp_secret),
        )
        PasslyOutlinedTextField(
            value = state.issuer,
            onValueChange = onEvent.onIssuerChange,
            label = stringResource(R.string.otp_issuer),
        )
        PasslyOutlinedTextField(
            value = state.accountName,
            onValueChange = onEvent.onAccountNameChange,
            label = stringResource(R.string.otp_account_name),
        )
        DropdownSelector(
            value = state.algorithm,
            onValueChange = onEvent.onAlgorithmChange,
            options = OtpEditorAlgorithm.entries,
            label = stringResource(R.string.totp_algorithm),
            enabled = state.type != OtpEditorType.STEAM,
            optionToString = { it.name },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.type == OtpEditorType.HOTP) {
                PasslyOutlinedTextField(
                    value = state.counter,
                    onValueChange = onEvent.onCounterChange,
                    label = stringResource(R.string.otp_counter),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            } else {
                PasslyOutlinedTextField(
                    value = state.period,
                    onValueChange = onEvent.onPeriodChange,
                    label = stringResource(R.string.totp_period),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            PasslyOutlinedTextField(
                value = state.digits,
                onValueChange = onEvent.onDigitsChange,
                label = stringResource(R.string.totp_digits),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = state.type != OtpEditorType.STEAM,
            )
        }
        DropdownSelector(
            value = state.encoding,
            onValueChange = onEvent.onEncodingChange,
            options = OtpEditorEncoding.entries,
            label = stringResource(R.string.otp_secret_encoding),
            modifier = Modifier.fillMaxWidth(),
            optionToString = { it.name },
        )
    }
}

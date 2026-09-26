package com.aozijx.passly.presentation.feature.vault.editor.ui.otp

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
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.presentation.feature.vault.editor.otp.AddOtpAction
import com.aozijx.passly.presentation.feature.vault.editor.otp.OtpFormState
import com.aozijx.passly.presentation.shared.components.NextFocusTextField
import com.aozijx.passly.presentation.shared.components.common.DropdownSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpConfigForm(
    state: OtpFormState,
    onAction: (AddOtpAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DropdownSelector(
            value = state.type,
            onValueChange = { onAction(AddOtpAction.TypeChanged(it)) },
            options = OtpType.entries,
            label = stringResource(R.string.otp_type),
            optionToString = { it.name },
        )
        NextFocusTextField(
            value = state.secret,
            onValueChange = { onAction(AddOtpAction.FormChanged(state.copy(secret = it))) },
            label = stringResource(R.string.totp_secret),
        )
        NextFocusTextField(
            value = state.issuer,
            onValueChange = { onAction(AddOtpAction.FormChanged(state.copy(issuer = it))) },
            label = stringResource(R.string.otp_issuer),
        )
        NextFocusTextField(
            value = state.accountName,
            onValueChange = { onAction(AddOtpAction.FormChanged(state.copy(accountName = it))) },
            label = stringResource(R.string.otp_account_name),
        )
        DropdownSelector(
            value = OtpHashAlgorithm.entries.firstOrNull { it.name == state.algorithm }
                ?: OtpHashAlgorithm.SHA1,
            onValueChange = {
                onAction(AddOtpAction.FormChanged(state.copy(algorithm = it.name)))
            },
            options = OtpHashAlgorithm.entries,
            label = stringResource(R.string.totp_algorithm),
            enabled = state.type != OtpType.STEAM,
            optionToString = { it.name },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.type == OtpType.HOTP) {
                NextFocusTextField(
                    value = state.counter,
                    onValueChange = {
                        onAction(AddOtpAction.FormChanged(state.copy(counter = it)))
                    },
                    label = stringResource(R.string.otp_counter),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                )
            } else {
                NextFocusTextField(
                    value = state.period,
                    onValueChange = {
                        onAction(AddOtpAction.FormChanged(state.copy(period = it)))
                    },
                    label = stringResource(R.string.totp_period),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                )
            }
            NextFocusTextField(
                value = state.digits,
                onValueChange = {
                    onAction(AddOtpAction.FormChanged(state.copy(digits = it)))
                },
                label = stringResource(R.string.totp_digits),
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                enabled = state.type != OtpType.STEAM,
            )
        }
        DropdownSelector(
            value = state.encoding,
            onValueChange = {
                onAction(AddOtpAction.FormChanged(state.copy(encoding = it)))
            },
            options = OtpSecretEncoding.entries,
            label = stringResource(R.string.otp_secret_encoding),
            modifier = Modifier.fillMaxWidth(),
            optionToString = { it.name },
        )
    }
}

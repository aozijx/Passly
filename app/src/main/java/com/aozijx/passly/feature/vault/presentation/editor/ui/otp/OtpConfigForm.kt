package com.aozijx.passly.feature.vault.presentation.editor.ui.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.presentation.editor.otp.AddOtpAction
import com.aozijx.passly.feature.vault.presentation.editor.otp.OtpFieldFeedbackUiState
import com.aozijx.passly.feature.vault.presentation.editor.otp.OtpFormState
import com.aozijx.passly.feature.vault.presentation.editor.otp.labelResource
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.NextFocusTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpConfigForm(
    state: OtpFormState,
    onAction: (AddOtpAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = state.feedback
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DropdownSelector(
            value = state.type,
            onValueChange = { onAction(AddOtpAction.TypeChanged(it)) },
            options = OtpType.entries,
            label = stringResource(R.string.otp_type),
            optionLabel = { stringResource(it.labelResource) },
        )
        NextFocusTextField(
            value = state.secret,
            onValueChange = { onAction(AddOtpAction.SecretChanged(it)) },
            label = stringResource(R.string.totp_secret),
            isError = feedback.secret.isInvalid,
            supportingText = feedback.secret.supportingText(R.string.otp_secret_invalid),
        )
        NextFocusTextField(
            value = state.issuer,
            onValueChange = { onAction(AddOtpAction.IssuerChanged(it)) },
            label = stringResource(R.string.otp_issuer),
        )
        NextFocusTextField(
            value = state.accountName,
            onValueChange = { onAction(AddOtpAction.AccountNameChanged(it)) },
            label = stringResource(R.string.otp_account_name),
        )
        DropdownSelector(
            value = state.algorithm,
            onValueChange = { onAction(AddOtpAction.AlgorithmChanged(it)) },
            options = OtpHashAlgorithm.entries,
            label = stringResource(R.string.totp_algorithm),
            enabled = state.allowsParameterEditing,
            optionLabel = { stringResource(it.labelResource) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.usesCounterField) {
                NextFocusTextField(
                    value = state.counter,
                    onValueChange = { onAction(AddOtpAction.CounterChanged(it)) },
                    label = stringResource(R.string.otp_counter),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    isError = feedback.counter.isInvalid,
                    supportingText = feedback.counter.supportingText(R.string.otp_counter_invalid),
                )
            } else {
                NextFocusTextField(
                    value = state.period,
                    onValueChange = { onAction(AddOtpAction.PeriodChanged(it)) },
                    label = stringResource(R.string.totp_period),
                    enabled = state.allowsParameterEditing,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    isError = feedback.period.isInvalid,
                    supportingText = feedback.period.supportingText(R.string.otp_period_invalid),
                )
            }
            NextFocusTextField(
                value = state.digits,
                onValueChange = { onAction(AddOtpAction.DigitsChanged(it)) },
                label = stringResource(R.string.totp_digits),
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                enabled = state.allowsParameterEditing,
                isError = feedback.digits.isInvalid,
                supportingText = feedback.digits.supportingText(R.string.otp_digits_invalid),
            )
        }
        DropdownSelector(
            value = state.encoding,
            onValueChange = { onAction(AddOtpAction.EncodingChanged(it)) },
            options = OtpSecretEncoding.entries,
            label = stringResource(R.string.otp_secret_encoding),
            modifier = Modifier.fillMaxWidth(),
            optionLabel = { stringResource(it.labelResource) },
        )
    }
}

private val OtpFieldFeedbackUiState.isInvalid: Boolean
    get() = this == OtpFieldFeedbackUiState.Invalid

@Composable
private fun OtpFieldFeedbackUiState.supportingText(messageRes: Int): (@Composable () -> Unit)? =
    when (this) {
        OtpFieldFeedbackUiState.Hidden -> {
            null
        }

        OtpFieldFeedbackUiState.Invalid -> {
            { Text(stringResource(messageRes)) }
        }
    }

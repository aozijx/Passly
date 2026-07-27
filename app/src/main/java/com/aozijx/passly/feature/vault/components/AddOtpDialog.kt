package com.aozijx.passly.feature.vault.components

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.ui.components.AppDialog
import com.aozijx.passly.core.ui.components.AppTextField
import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.feature.vault.VaultViewModel

@Composable
fun AddOtpDialog(
    viewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    val context = LocalContext.current
    val formState by viewModel.otpFormState.collectAsState()

    val uriParsedMsg = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMsg = stringResource(R.string.vault_otp_uri_parse_failed)

    LaunchedEffect(formState.uriText) {
        val parsed = TotpUtils.parseOtpAuthUri(formState.uriText) ?: return@LaunchedEffect
        try {
            val accountName = parsed.accountName ?: ""
            val username =
                (parsed.accountName?.split(":")?.getOrNull(1)?.trim() ?: parsed.accountName) ?: ""
            val issuer = parsed.issuer ?: ""

            viewModel.setOtpFormState(
                formState.copy(
                    title = accountName,
                    username = username,
                    secret = parsed.secret,
                    issuer = issuer,
                    domain = issuer,
                    digits = parsed.digits.toString(),
                    period = (parsed.periodSeconds ?: 30).toString(),
                    counter = (parsed.counter ?: 0L).toString(),
                    algorithm = parsed.algorithm.name,
                    encoding = parsed.encoding
                )
            )
            viewModel.updateOtpType(parsed.type)

            Toast.makeText(context, uriParsedMsg, Toast.LENGTH_SHORT).show()
            ClipboardUtils.clear(context)
        } catch (e: Exception) {
            AppTelemetry.e("AddTwoFA", uriParseFailedMsg, e)
        }
    }

    AppDialog(
        title = stringResource(R.string.vault_add_otp_title),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = formState.isValid,
        onConfirm = { viewModel.saveOtpEntry() }) {
        AppTextField(
            value = formState.title,
            onValueChange = {
                viewModel.setOtpFormState(formState.copy(title = it))
                onUpdateInteraction()
            },
            label = stringResource(R.string.title)
        )

        AppTextField(
            value = formState.uriText,
            onValueChange = {
                viewModel.setOtpFormState(formState.copy(uriText = it))
                onUpdateInteraction()
            },
            label = stringResource(R.string.twofa_uri_hint),
            trailingIcon = {
                TextButton(onClick = {
                    val text = ClipboardUtils.getText(context)
                    viewModel.setOtpFormState(formState.copy(uriText = text))
                }) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.paste))
                }
            })

        OtpConfigForm(
            state = formState,
            onFieldUpdate = { viewModel.setOtpFormState(it) },
            onTypeChange = { viewModel.updateOtpType(it) }
        )
    }
}

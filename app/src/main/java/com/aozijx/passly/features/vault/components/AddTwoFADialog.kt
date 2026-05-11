package com.aozijx.passly.features.vault.components

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.base.BaseVaultDialog
import com.aozijx.passly.core.designsystem.fields.CategoryDropdown
import com.aozijx.passly.core.designsystem.fields.VaultTextField
import com.aozijx.passly.core.designsystem.sections.TotpConfigForm
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.security.otp.TotpUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun AddTwoFADialog(
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val state = remember { TotpAddState() }
    val algorithms = listOf("SHA1", "SHA256", "SHA512", "STEAM")

    val uriParsedMsg = stringResource(R.string.vault_2fa_uri_parsed)
    val uriParseFailedMsg = stringResource(R.string.vault_2fa_uri_parse_failed)
    val otpCategory = stringResource(R.string.category_otp)

    LaunchedEffect(state.uriText) {
        val parsed = TotpUtils.parseOtpAuthUri(state.uriText) ?: return@LaunchedEffect
        try {
            state.title = parsed.issuer ?: parsed.label.split(":").firstOrNull() ?: ""
            state.username = parsed.label
            state.secret = parsed.secret
            state.issuer = parsed.issuer ?: ""
            state.domain = parsed.issuer ?: ""

            val isSteam = (parsed.issuer ?: parsed.label).contains("Steam", ignoreCase = true)
            if (isSteam) {
                state.algorithm = "STEAM"
                state.digits = "5"
            } else {
                state.algorithm = parsed.algorithm?.takeIf { algorithms.contains(it) } ?: "SHA1"
                state.digits = (parsed.digits ?: 6).toString()
            }
            state.period = (parsed.period ?: 30).toString()

            Toast.makeText(context, uriParsedMsg, Toast.LENGTH_SHORT).show()
            ClipboardUtils.clear(context)
        } catch (e: Exception) {
            Logcat.e("AddTwoFA", uriParseFailedMsg, e)
        }
    }

    BaseVaultDialog(
        title = stringResource(R.string.vault_add_2fa_title),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.isValid,
        onConfirm = {
            try {
                val entry = VaultEntry(
                    title = state.title,
                    username = state.username,
                    password = "",
                    category = state.category.ifBlank { otpCategory },
                    totpSecret = state.secret.trim(),
                    totpIssuer = state.issuer.ifBlank { null },
                    totpDigits = state.digits.toIntOrNull() ?: 6,
                    totpPeriod = state.period.toIntOrNull() ?: 30,
                    totpAlgorithm = state.algorithm,
                    associatedDomain = state.domain.ifBlank { null },
                    entryType = 1
                )
                viewModel.addItem(entry, state.domain)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                Logcat.e("AddTwoFA", "Failed to encrypt/save", e)
                Toast.makeText(context, "加密保存失败", Toast.LENGTH_SHORT).show()
            }
        }) {
        VaultTextField(
            value = state.title,
            onValueChange = { state.title = it },
            label = stringResource(R.string.label_title)
        )

        VaultTextField(
            value = state.uriText,
            onValueChange = { state.uriText = it },
            label = stringResource(R.string.label_2fa_uri_hint),
            trailingIcon = {
                TextButton(onClick = { state.uriText = ClipboardUtils.getText(context) }) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.action_paste))
                }
            })

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = viewModel.availableCategories.collectAsStateWithLifecycle().value
        )

        if (state.showAdvanced) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TotpConfigForm(
                secret = state.secret,
                onSecretChange = { state.secret = it },
                period = state.period,
                onPeriodChange = { state.period = it },
                digits = state.digits,
                onDigitsChange = { state.digits = it },
                algorithm = state.algorithm,
                onAlgorithmChange = { state.algorithm = it })
        } else {
            TextButton(onClick = { state.showAdvanced = true }) {
                Text(stringResource(R.string.action_advanced_config))
            }
        }
    }
}
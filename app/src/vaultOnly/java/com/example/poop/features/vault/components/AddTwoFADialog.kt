package com.example.poop.features.vault.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.poop.R
import com.example.poop.core.common.AddType
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.core.designsystem.base.BaseVaultDialog
import com.example.poop.core.designsystem.fields.CategoryDropdown
import com.example.poop.core.designsystem.fields.VaultTextField
import com.example.poop.core.designsystem.sections.TotpConfigForm
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.TotpAddState
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.Logcat
import java.net.URLDecoder

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

    // 自动解析 otpauth URI 逻辑
    LaunchedEffect(state.uriText) {
        if (!state.uriText.startsWith("otpauth://totp/")) return@LaunchedEffect
        try {
            val uri = state.uriText.toUri()
            val label = URLDecoder.decode(uri.path?.trimStart('/') ?: "", "UTF-8")
            val secret = uri.getQueryParameter("secret") ?: ""
            val issuer = uri.getQueryParameter("issuer")
            val rawAlgorithm = uri.getQueryParameter("algorithm")?.uppercase()
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

            state.title = issuer ?: label.split(":").firstOrNull() ?: ""
            state.username = label
            state.secret = secret

            if ((issuer ?: label).contains("Steam", ignoreCase = true)) {
                state.algorithm = "STEAM"
                state.digits = "5"
            } else {
                state.algorithm = rawAlgorithm?.takeIf { algorithms.contains(it) } ?: "SHA1"
                state.digits = digits.toString()
            }
            state.period = period.toString()

            Toast.makeText(context, uriParsedMsg, Toast.LENGTH_SHORT).show()
            ClipboardUtils.clear(context)
        } catch (e: Exception) {
            Logcat.e("AddTwoFA", uriParseFailedMsg, e)
        }
    }

    BaseVaultDialog(
        title = stringResource(R.string.vault_add_2fa_title),
        onDismiss = { viewModel.addType = AddType.NONE },
        confirmEnabled = state.isValid,
        onConfirm = {
            try {
                val encryptedSecret = CryptoManager.encrypt(state.secret.trim())
                val entry = VaultEntry(
                    title = state.title,
                    username = state.username,
                    password = "",
                    category = state.category.ifBlank { otpCategory },
                    totpSecret = encryptedSecret,
                    totpDigits = state.digits.toIntOrNull() ?: 6,
                    totpPeriod = state.period.toIntOrNull() ?: 30,
                    totpAlgorithm = state.algorithm,
                    entryType = 1
                )
                viewModel.addItem(entry)
                viewModel.addType = AddType.NONE
            } catch (e: Exception) {
                Logcat.e("AddTwoFA", "Failed to encrypt/save", e)
                Toast.makeText(context, "加密保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
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
            }
        )

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = viewModel.availableCategories.collectAsState().value
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
                onAlgorithmChange = { state.algorithm = it }
            )
        } else {
            TextButton(onClick = { state.showAdvanced = true }) {
                Text(stringResource(R.string.action_advanced_config))
            }
        }
    }
}

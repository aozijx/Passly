package com.aozijx.passly.feature.vault.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.domain.model.credential.VaultCredential
import com.aozijx.passly.domain.model.credential.twofactor.TwoFactorConfig
import com.aozijx.passly.domain.model.credential.twofactor.TwoFactorType
import com.aozijx.passly.domain.model.credential.twofactor.otp.OtpConfig
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.ui.components.AppDialog
import com.aozijx.passly.ui.components.AppTextField

@Composable
fun AddTwoFADialog(
    viewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val state = remember { TotpAddState() }
    val algorithms = listOf("SHA1", "SHA256", "SHA512", "STEAM")

    val uriParsedMsg = stringResource(R.string.vault_2fa_uri_parsed)
    val uriParseFailedMsg = stringResource(R.string.vault_2fa_uri_parse_failed)

    LaunchedEffect(state.uriText) {
        val parsed = TotpUtils.parseOtpAuthUri(state.uriText) ?: return@LaunchedEffect
        try {
            // 优先将 Label 映射为标题，并尝试从 Label 中分离出账号部分
            state.title = parsed.label
            state.username = parsed.label.split(":").getOrNull(1)?.trim() ?: parsed.label

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
            AppLog.e("AddTwoFA", uriParseFailedMsg, e)
        }
    }

    AppDialog(
        title = stringResource(R.string.vault_add_2fa_title),
        onDismiss = { viewModel.setAddType(null) },
        confirmEnabled = state.isValid,
        onConfirm = {
            try {
                val entry = VaultEntry(
                    metadata = VaultMetadata(
                        entryId = "",
                        entryType = EntryType.LOGIN,
                        title = state.title,
                        username = state.username,
                        icon = null,
                        website = state.domain.ifBlank { null }?.let { WebsiteInfo(primaryUrl = it) }
                    ),
                    credential = VaultCredential(
                        entryId = "",
                        password = "",
                        twoFactor = TwoFactorConfig(
                            type = TwoFactorType.TOTP,
                            otp = OtpConfig(
                                secret = state.secret.trim(),
                                digits = state.digits.toIntOrNull() ?: 6,
                                period = state.period.toIntOrNull() ?: 30,
                                algorithm = state.algorithm,
                                issuer = state.issuer.ifBlank { null }
                            )
                        )
                    )
                )
                viewModel.addItem(entry, state.domain)
                viewModel.setAddType(null)
            } catch (e: Exception) {
                AppLog.e("AddTwoFA", "Failed to encrypt/save", e)
                Toast.makeText(context, "加密保存失败", Toast.LENGTH_SHORT).show()
            }
        }) {
        AppTextField(
            value = state.title,
            onValueChange = { state.title = it; onUpdateInteraction() },
            label = stringResource(R.string.title)
        )

        AppTextField(
            value = state.uriText,
            onValueChange = { state.uriText = it; onUpdateInteraction() },
            label = stringResource(R.string.twofa_uri_hint),
            trailingIcon = {
                TextButton(onClick = { state.uriText = ClipboardUtils.getText(context) }) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.paste))
                }
            })

        CategoryDropdown(
            selectedCategory = state.category,
            onCategorySelected = { state.category = it },
            availableCategories = uiState.availableCategories
        )

        if (state.showAdvanced) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TotpConfigForm(
                secret = state.secret,
                onSecretChange = { state.secret = it; onUpdateInteraction() },
                period = state.period,
                onPeriodChange = { state.period = it; onUpdateInteraction() },
                digits = state.digits,
                onDigitsChange = { state.digits = it; onUpdateInteraction() },
                algorithm = state.algorithm,
                onAlgorithmChange = { state.algorithm = it; onUpdateInteraction() })
        } else {
            TextButton(onClick = { state.showAdvanced = true }) {
                Text(stringResource(R.string.advanced_config))
            }
        }
    }
}
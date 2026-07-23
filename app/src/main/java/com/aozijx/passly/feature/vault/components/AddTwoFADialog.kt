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
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.entry.secret.OtpSecret
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpType
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

    val uriParsedMsg = stringResource(R.string.vault_2fa_uri_parsed)
    val uriParseFailedMsg = stringResource(R.string.vault_2fa_uri_parse_failed)

    LaunchedEffect(state.uriText) {
        val parsed = TotpUtils.parseOtpAuthUri(state.uriText) ?: return@LaunchedEffect
        try {
            // 优先将 accountName 映射为标题，并尝试从 accountName 中分离出账号部分
            state.title = parsed.accountName ?: ""
            state.username =
                (parsed.accountName?.split(":")?.getOrNull(1)?.trim() ?: parsed.accountName) ?: ""

            state.secret = parsed.secret
            state.issuer = parsed.issuer ?: ""
            state.domain = parsed.issuer ?: ""

            state.selectType(parsed.type)
            state.digits = parsed.digits.toString()
            state.period = (parsed.periodSeconds ?: 30).toString()
            state.counter = (parsed.counter ?: 0L).toString()
            state.algorithm = parsed.algorithm.name
            state.encoding = parsed.encoding

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
                    EntryHeader(
                        id = EntryId(""),
                        entryType = EntryType.LOGIN,
                        version = EntryVersion.INITIAL,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    EntrySummary(
                        title = state.title,
                        username = state.username,
                        icon = null,
                        website = state.domain.ifBlank { null }?.let { WebsiteInfo(primaryUrl = it) }
                    ),
                    EntrySecret.Otp(
                        OtpSecret(
                            config = OtpConfig(
                            type = state.type,
                            secret = state.secret.trim(),
                            digits = if (state.type == OtpType.STEAM) 5
                            else (state.digits.toIntOrNull() ?: 6),
                            periodSeconds = if (state.type == OtpType.HOTP) null
                            else (state.period.toIntOrNull() ?: 30),
                            counter = if (state.type == OtpType.HOTP) {
                                state.counter.toLongOrNull() ?: 0L
                            } else null,
                            algorithm = when (state.algorithm.uppercase()) {
                                "SHA256" -> OtpHashAlgorithm.SHA256
                                "SHA512" -> OtpHashAlgorithm.SHA512
                                else -> OtpHashAlgorithm.SHA1
                            },
                            encoding = state.encoding,
                            issuer = state.issuer.ifBlank { null },
                            accountName = state.username.ifBlank { null }
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
                type = state.type,
                onTypeChange = { state.selectType(it); onUpdateInteraction() },
                secret = state.secret,
                onSecretChange = { state.secret = it; onUpdateInteraction() },
                encoding = state.encoding,
                onEncodingChange = { state.encoding = it; onUpdateInteraction() },
                period = state.period,
                onPeriodChange = { state.period = it; onUpdateInteraction() },
                counter = state.counter,
                onCounterChange = { state.counter = it; onUpdateInteraction() },
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

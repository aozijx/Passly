package com.aozijx.passly.features.vault.components.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

object CardStyleRegistry {
    private val previewBaseEntry = VaultSummary(
        id = -100,
        title = "示例账号",
        category = "自动填充",
        username = "demo_user",
        associatedDomain = "example.com"
    )

    private val previewPasswordEntry = VaultSummary(
        id = -101,
        title = "我的邮箱",
        category = "登录凭据",
        username = "me@example.com",
        associatedDomain = "example.com"
    )

    private val previewTotpEntry = VaultSummary(
        id = -102,
        title = "示例二步验证",
        category = "OTP",
        username = "totp_user",
        totpSecret = "preview_totp"
    )

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: VaultSummary,
        viewModel: VaultViewModel,
        onClick: () -> Unit = { viewModel.showDetail(entry) }
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val isTotp = entry.totpSecret?.isNotBlank() == true
        val isAutofill = entry.category == stringResource(R.string.category_autofill)

        when (style) {
            VaultCardStyle.DEFAULT -> {
                when {
                    isTotp -> {
                        TwoFAItem(
                            entry = entry,
                            vaultViewModel = viewModel,
                            showCode = uiState.showTOTPCode,
                            onClick = onClick
                        )
                    }

                    isAutofill -> {
                        AutoFillItem(
                            entry = entry,
                            viewModel = viewModel,
                            onClick = onClick
                        )
                    }

                    else -> {
                        VaultItem(entry = entry, onClick = onClick)
                    }
                }
            }

            VaultCardStyle.PASSWORD -> {
                PasswordStyleVaultItem(
                    entry = entry, viewModel = viewModel, onClick = onClick
                )
            }

            VaultCardStyle.TOTP -> {
                if (isTotp) {
                    TotpStyleVaultItem(
                        entry = entry,
                        vaultViewModel = viewModel,
                        showCode = uiState.showTOTPCode,
                        onClick = onClick
                    )
                } else {
                    VaultItem(entry = entry, onClick = onClick)
                }
            }
        }
    }

    @Composable
    fun RenderPreviewVaultItem(
        style: VaultCardStyle, entryTypeValue: Int? = null, onClick: () -> Unit
    ) {
        val isTotp = entryTypeValue == EntryType.TOTP.value

        when (style) {
            VaultCardStyle.DEFAULT -> {
                if (isTotp) {
                    TwoFAItem(
                        entry = previewTotpEntry,
                        vaultViewModel = null,
                        showCode = true,
                        previewCode = "123 456",
                        previewProgress = 0.4f,
                        onClick = onClick
                    )
                } else {
                    VaultItem(entry = previewBaseEntry, onClick = onClick)
                }
            }

            VaultCardStyle.PASSWORD -> {
                PasswordStyleVaultItem(
                    entry = previewPasswordEntry, viewModel = null, onClick = onClick
                )
            }

            VaultCardStyle.TOTP -> {
                TotpStyleVaultItem(
                    entry = previewTotpEntry,
                    vaultViewModel = null,
                    showCode = true,
                    previewCode = "123 456",
                    previewProgress = 0.4f,
                    onClick = onClick
                )
            }
        }
    }
}
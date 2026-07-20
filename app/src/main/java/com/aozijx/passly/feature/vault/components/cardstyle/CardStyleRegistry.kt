package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.otp.TotpState
import com.aozijx.passly.domain.model.credential.VaultCredential
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.settings.VaultCardStyle

object CardStyleRegistry {
    private val previewBaseEntry = VaultEntry(
        metadata = VaultMetadata(
            entryId = "-100",
            entryType = EntryType.LOGIN,
            title = "示例账号",
            username = "demo_user",
            icon = null,
            website = WebsiteInfo(primaryUrl = "example.com")
        ),
        credential = VaultCredential(entryId = "-100")
    )

    private val previewPasswordEntry = VaultEntry(
        metadata = VaultMetadata(
            entryId = "-101",
            entryType = EntryType.LOGIN,
            title = "我的邮箱",
            username = "me@example.com",
            icon = null,
            website = WebsiteInfo(primaryUrl = "example.com")
        ),
        credential = VaultCredential(entryId = "-101")
    )

    private val previewTotpEntry = VaultEntry(
        metadata = VaultMetadata(
            entryId = "-102",
            entryType = EntryType.LOGIN,
            title = "示例二步验证",
            username = "totp_user",
            icon = null
        ),
        credential = VaultCredential(entryId = "-102")
    )

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: VaultEntry,
        totpStates: Map<String, TotpState>,
        showTotpCode: Boolean,
        onClick: () -> Unit
    ) {
        val isTotp = entry.credential.twoFactor?.otp?.secret?.isNotBlank() == true
        val isAutofill = entry.category == stringResource(R.string.category_autofill)
        val totpState = totpStates[entry.id]

        when (style) {
            VaultCardStyle.DEFAULT -> {
                when {
                    isTotp -> {
                        TwoFAItem(
                            entry = entry,
                            totpState = totpState,
                            showCode = showTotpCode,
                            onClick = onClick
                        )
                    }

                    isAutofill -> {
                        AutoFillItem(
                            entry = entry,
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
                    entry = entry, onClick = onClick
                )
            }

            VaultCardStyle.TOTP -> {
                if (isTotp) {
                    TotpStyleVaultItem(
                        entry = entry,
                        totpState = totpState,
                        showCode = showTotpCode,
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
        style: VaultCardStyle, onClick: () -> Unit
    ) {
        val isTotp = false

        when (style) {
            VaultCardStyle.DEFAULT -> {
                if (isTotp) {
                    TwoFAItem(
                        entry = previewTotpEntry,
                        totpState = null,
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
                    entry = previewPasswordEntry, onClick = onClick
                )
            }

            VaultCardStyle.TOTP -> {
                TotpStyleVaultItem(
                    entry = previewTotpEntry,
                    totpState = null,
                    showCode = true,
                    previewCode = "123 456",
                    previewProgress = 0.4f,
                    onClick = onClick
                )
            }
        }
    }
}
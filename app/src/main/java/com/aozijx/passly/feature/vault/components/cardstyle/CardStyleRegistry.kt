package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.feature.vault.model.TotpState

object CardStyleRegistry {
    private val previewBaseEntry = VaultListItem(
        id = "-100",
        entryType = EntryType.LOGIN,
        title = "示例账号",
        username = "demo_user",
        icon = null,
        iconCustomPath = null,
        website = WebsiteInfo(primaryUrl = "example.com"),
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 0,
        hasTotp = false
    )

    private val previewPasswordEntry = VaultListItem(
        id = "-101",
        entryType = EntryType.LOGIN,
        title = "我的邮箱",
        username = "me@example.com",
        icon = null,
        iconCustomPath = null,
        website = WebsiteInfo(primaryUrl = "example.com"),
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 0,
        hasTotp = false
    )

    private val previewTotpEntry = VaultListItem(
        id = "-102",
        entryType = EntryType.LOGIN,
        title = "示例二步验证",
        username = "totp_user",
        icon = null,
        iconCustomPath = null,
        website = null,
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 0,
        hasTotp = true
    )

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: VaultListItem,
        totpStates: Map<String, TotpState>,
        showTotpCode: Boolean,
        onClick: () -> Unit
    ) {
        val isTotp = entry.hasTotp
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
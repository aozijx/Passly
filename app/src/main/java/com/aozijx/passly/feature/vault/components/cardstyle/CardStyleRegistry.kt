package com.aozijx.passly.feature.vault.components.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.VaultCardStyle
import com.aozijx.passly.feature.vault.model.OtpUiState

object CardStyleRegistry {
    /**
     * Resolves one primary list-card style for an entry.
     *
     * Detail-page capability composition is intentionally handled elsewhere.
     * A mixed login + OTP entry still occupies one list row and uses its
     * primary entry type's presentation.
     */
    fun resolveStyle(
        entry: EntryListItem,
        presentations: List<EntryCardPresentation>,
    ): VaultCardStyle {
        val presentation = presentations.firstOrNull {
            it.entryTypeKey.equals(entry.entryType.name, ignoreCase = true)
        }
        val requested = VaultCardStyle.fromKey(presentation?.variantKey)
        return if (requested == VaultCardStyle.TOTP && !entry.hasOtp) {
            VaultCardStyle.DEFAULT
        } else {
            requested
        }
    }

    private val previewBaseEntry = EntryListItem(
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
        capabilityFlags = 0,
        otpTypeName = ""
    )

    private val previewPasswordEntry = EntryListItem(
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
        capabilityFlags = 0,
        otpTypeName = ""
    )

    private val previewTotpEntry = EntryListItem(
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
        capabilityFlags = EntryCapabilityFlags.HAS_OTP,
        otpTypeName = "TOTP"
    )

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: EntryListItem,
        totpState: OtpUiState?,
        showTotpCode: Boolean,
        onClick: () -> Unit
    ) {
        val isTotp = entry.hasOtp
        val isAutofill = entry.category == stringResource(R.string.category_autofill)

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

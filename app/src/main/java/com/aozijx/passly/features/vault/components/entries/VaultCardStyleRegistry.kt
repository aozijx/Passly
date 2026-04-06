package com.aozijx.passly.features.vault.components.entries

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.base.VaultItem
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.components.items.TwoFAItem

/**
 * Single source of truth for card-style availability and rendering.
 */
object VaultCardStyleRegistry {
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
        val isTotp = entry.totpSecret?.isNotBlank() == true

        // 核心渲染分发：
        // 1. DEFAULT 的最终落地样式由 UiTypes.resolveForEntryType 预先决策
        // 2. 这里仅按传入的最终样式做渲染分发

        when (style) {
            VaultCardStyle.DEFAULT -> {
                if (isTotp) {
                    TwoFAItem(
                        entry = entry,
                        vaultViewModel = viewModel,
                        showCode = viewModel.showTOTPCode,
                        onClick = onClick
                    )
                } else {
                    VaultItem(entry = entry, viewModel = viewModel, onClick = onClick)
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
                        showCode = viewModel.showTOTPCode,
                        onClick = onClick
                    )
                } else {
                    // 非 TOTP 条目选了 TOTP 样式，回退到基础款
                    VaultItem(entry = entry, viewModel = viewModel, onClick = onClick)
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
                    VaultItem(entry = previewBaseEntry, viewModel = null, onClick = onClick)
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

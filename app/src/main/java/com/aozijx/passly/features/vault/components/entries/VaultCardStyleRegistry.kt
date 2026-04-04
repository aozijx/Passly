package com.aozijx.passly.features.vault.components.entries

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.core.designsystem.base.VaultItem
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

/**
 * Single source of truth for card-style availability and rendering.
 */
object VaultCardStyleRegistry {
    private val previewBaseEntry = VaultSummary(
        id = -100,
        title = "示例账号",
        category = "自动填充",
        username = "demo_user",
        password = "demo_password",
        associatedDomain = "example.com"
    )

    private val previewPasswordEntry = VaultSummary(
        id = -101,
        title = "我的邮箱",
        category = "登录凭据",
        username = "me@example.com",
        password = "********",
        associatedDomain = "example.com"
    )

    private val previewTotpEntry = VaultSummary(
        id = -102,
        title = "示例二步验证",
        category = "OTP",
        username = "totp_user",
        password = "",
        totpSecret = "preview_totp"
    )

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: VaultSummary,
        viewModel: VaultViewModel,
        onClick: () -> Unit = { viewModel.showDetail(entry) }
    ) {
        if (entry.totpSecret?.isNotBlank() == true) {
            when (style) {
                VaultCardStyle.TOTP -> {
                    TotpStyleVaultItem(
                        entry = entry,
                        vaultViewModel = viewModel,
                        showCode = viewModel.showTOTPCode,
                        onClick = onClick
                    )
                }

                else -> {
                    VaultItem(
                        entry = entry,
                        viewModel = viewModel,
                        onClick = onClick
                    )
                }
            }
            return
        }

        when (style) {
            VaultCardStyle.PASSWORD -> PasswordStyleVaultItem(
                entry = entry,
                viewModel = viewModel,
                onClick = onClick
            )

            VaultCardStyle.TOTP -> VaultItem(
                entry = entry,
                viewModel = viewModel,
                onClick = onClick
            )

            VaultCardStyle.DEFAULT -> VaultItem(
                entry = entry,
                viewModel = viewModel,
                onClick = onClick
            )

            VaultCardStyle.BASE -> VaultItem(
                entry = entry,
                viewModel = viewModel,
                onClick = onClick
            )
        }
    }

    @Composable
    fun RenderPreviewVaultItem(
        style: VaultCardStyle,
        onClick: () -> Unit
    ) {
        when (style) {
            VaultCardStyle.PASSWORD -> PasswordStyleVaultItem(
                entry = previewPasswordEntry,
                viewModel = null,
                onClick = onClick
            )

            VaultCardStyle.TOTP -> TotpStyleVaultItem(
                entry = previewTotpEntry,
                vaultViewModel = null,
                showCode = true,
                previewCode = "123 456",
                previewProgress = 0.4f,
                onClick = onClick
            )

            VaultCardStyle.DEFAULT,
            VaultCardStyle.BASE -> VaultItem(
                entry = previewBaseEntry,
                viewModel = null,
                onClick = onClick
            )
        }
    }

}

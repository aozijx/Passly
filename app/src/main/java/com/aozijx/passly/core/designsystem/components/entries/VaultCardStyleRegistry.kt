package com.aozijx.passly.core.designsystem.components.entries

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.core.designsystem.base.VaultItem
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

/**
 * Single source of truth for card-style availability and rendering.
 */
object VaultCardStyleRegistry {
    val settingsStyles: List<VaultCardStyle> = listOf(VaultCardStyle.BASE, VaultCardStyle.PASSWORD)
    val defaultStyle: VaultCardStyle = VaultCardStyle.BASE

    fun resolveSettingsStyle(style: VaultCardStyle): VaultCardStyle {
        return if (style in settingsStyles) style else defaultStyle
    }

    @Composable
    fun RenderVaultItem(
        style: VaultCardStyle,
        entry: VaultSummary,
        viewModel: VaultViewModel,
        onClick: () -> Unit = { viewModel.showDetail(entry) }
    ) {
        when (style) {
            VaultCardStyle.PASSWORD -> PasswordStyleVaultItem(
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
    fun RenderPreview(style: VaultCardStyle, onClick: () -> Unit) {
        when (style) {
            VaultCardStyle.BASE -> {
                VaultItem(entry = basePreviewEntry, onClick = onClick)
            }

            VaultCardStyle.PASSWORD -> {
                PasswordStyleVaultItem(entry = passwordPreviewEntry, onClick = onClick)
            }
        }
    }

    private val basePreviewEntry = VaultSummary(
        id = -1,
        title = "示例账号",
        category = "自动填充",
        entryType = 0,
        username = "demo@example.com",
        password = "******"
    )

    private val passwordPreviewEntry = VaultSummary(
        id = -2,
        title = "我的邮箱",
        category = "账号密码",
        entryType = 0,
        username = "demo@example.com",
        password = "******",
        associatedDomain = "example.com"
    )

}

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

}

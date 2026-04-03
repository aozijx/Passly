package com.aozijx.passly.core.designsystem.components.entries

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.common.EntryType.Companion.fromValue
import com.aozijx.passly.core.designsystem.base.VaultItem
import com.aozijx.passly.core.designsystem.utils.EntryTypeResolver
import com.aozijx.passly.core.designsystem.utils.FieldKey
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

/**
 * 类型路由器：保持统一列表外观（复用 VaultItem），
 * 同时根据类型补充简短摘要信息。
 */
@Composable
fun TypedVaultItemRouter(
    entry: VaultSummary,
    viewModel: VaultViewModel,
    onClick: () -> Unit = { viewModel.showDetail(entry) }
) {
    val vaultType = fromValue(entry.entryType)
    val secondary = buildSecondarySummary(entry, vaultType)
    val displayEntry = if (secondary == null) {
        entry
    } else {
        entry.copy(category = "${entry.category} · $secondary")
    }

    VaultItem(
        entry = displayEntry,
        viewModel = viewModel,
        onClick = onClick
    )
}

private fun buildSecondarySummary(entry: VaultSummary, vaultType: com.aozijx.passly.core.common.EntryType): String? {
    return when (EntryTypeResolver.getSecondaryField(vaultType)) {
        FieldKey.URIS -> entry.associatedDomain?.takeIf { it.isNotBlank() }
        // VaultSummary 目前未携带这两个字段，保留分支便于后续扩展
        FieldKey.WIFI_ENCRYPTION -> null
        FieldKey.CARD_EXPIRATION -> null
        else -> null
    }
}

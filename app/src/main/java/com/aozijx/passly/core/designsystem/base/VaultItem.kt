package com.aozijx.passly.core.designsystem.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aozijx.passly.core.designsystem.model.VaultCardStyleTokens
import com.aozijx.passly.domain.model.icon.VaultIconInfo
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

/**
 * 保险库条目列表项：通用列表卡片
 * 点击后打开详情对话框 (VaultDetailDialog)
 */
@Composable
fun VaultItem(
    entry: VaultSummary,
    viewModel: VaultViewModel? = null,
    onClick: () -> Unit = { viewModel?.showDetail(entry) }
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VaultCardStyleTokens.Base.corner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = VaultCardStyleTokens.Base.CONTAINER_ALPHA
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(VaultCardStyleTokens.Base.contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultItemIcon(
                info = VaultIconInfo(
                    iconName = entry.iconName,
                    iconCustomPath = entry.iconCustomPath,
                    associatedDomain = entry.associatedDomain,
                    associatedAppPackage = entry.associatedAppPackage,
                    category = entry.category
                )
            )

            Spacer(modifier = Modifier.width(VaultCardStyleTokens.Base.iconTextSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



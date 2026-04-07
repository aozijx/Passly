package com.aozijx.passly.features.vault.components.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.icons.VaultIcons
import com.aozijx.passly.core.designsystem.icons.getCategoryIcon
import com.aozijx.passly.core.media.rememberResolvedVaultIcon
import com.aozijx.passly.core.platform.rememberAppIconPainter
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun AutoFillItem(
    entry: VaultSummary,
    viewModel: VaultViewModel,
    onClick: () -> Unit = { viewModel.showDetail(entry) }
) {
    val context = LocalContext.current
    // 判断是否为自动抓取的“静默”数据
    val isAutoCaptured = entry.category == stringResource(R.string.category_autofill)

    // 使用 rememberResolvedVaultIcon 解析图标来源 (演示该函数的用法)
    val resolvedIcon = rememberResolvedVaultIcon(
        customPath = entry.iconCustomPath,
        domain = entry.associatedDomain,
        packageName = entry.associatedAppPackage
    )

    // 获取兜底图标逻辑
    val fallbackIcon = remember(entry.iconName, entry.category) {
        val resId = entry.iconName?.toIntOrNull()
        when {
            resId != null -> VaultIcons.getIconByRes(resId)
            !entry.iconName.isNullOrEmpty() -> VaultIcons.getIconByName(entry.iconName)
            else -> getCategoryIcon(context, entry.category)
        }
    }
    val fallbackPainter = rememberVectorPainter(fallbackIcon)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (resolvedIcon.model != null) {
                    if (resolvedIcon.isPackage) {
                        val appPainter = rememberAppIconPainter(resolvedIcon.model as String)
                        if (appPainter != null) {
                            Image(
                                painter = appPainter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Icon(fallbackIcon, null, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        val modelString = resolvedIcon.model.toString()
                        val coilModel = when {
                            modelString.startsWith("http") || modelString.startsWith("file://") -> resolvedIcon.model
                            modelString.contains(".") -> "https://$modelString/favicon.ico"
                            else -> resolvedIcon.model
                        }

                        AsyncImage(
                            model = coilModel,
                            contentDescription = null,
                            placeholder = fallbackPainter,
                            error = fallbackPainter,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中间信息区
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 如果是自动抓取，显示安全警告小图标
                    if (isAutoCaptured) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Security,
                            contentDescription = stringResource(R.string.autofill_pending_verification),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

package com.aozijx.passly.core.designsystem.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.designsystem.icons.getCategoryIcon
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.media.ImageResolver.toLocalIconImageModel
import com.aozijx.passly.core.platform.rememberAppIcon
import com.aozijx.passly.domain.model.icon.VaultIconable

@Composable
fun VaultItemIcon(
    modifier: Modifier = Modifier,
    iconable: VaultIconable,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val context = LocalContext.current

    // 1. 获取包名图标 (异步加载)
    val appIconPainter = rememberAppIcon(iconable.associatedAppPackage)

    // 2. 获取分类兜底图标 (矢量图转 Painter)
    val fallbackIconVector = remember(iconable.iconName, iconable.category) {
        getCategoryIcon(context, iconable.category)
    }
    val fallbackPainter = rememberVectorPainter(fallbackIconVector)

    // 3. 确定占位符：有包名图标用包名，没有用分类兜底
    val placeholderPainter = appIconPainter ?: fallbackPainter

    // 4. 自定义/域名图标路径
    val customModel = remember(iconable.iconCustomPath) { toLocalIconImageModel(iconable.iconCustomPath) }
    val domainUrl = remember(iconable.associatedDomain) {
        iconable.associatedDomain?.let { "https://${FaviconUtils.cleanDomain(it)}/favicon.ico" }
    }

    Box(
        modifier = modifier.size(36.dp), contentAlignment = Alignment.Center
    ) {
        when {
            // 情况 A: 存在自定义图标或域名，使用 AsyncImage 渲染，并带上动态占位符
            customModel != null || domainUrl != null -> {
                AsyncImage(
                    model = customModel ?: domainUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholderPainter,
                    error = placeholderPainter
                )
            }

            // 情况 B: 没有自定义路径，但包名图标已加载成功
            appIconPainter != null -> {
                Image(
                    painter = appIconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            // 情况 C: 最终兜底，显示分类矢量图标
            else -> {
                Icon(
                    imageVector = fallbackIconVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
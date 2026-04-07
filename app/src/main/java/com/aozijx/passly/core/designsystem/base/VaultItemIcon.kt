package com.aozijx.passly.core.designsystem.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.designsystem.icons.VaultIcons
import com.aozijx.passly.core.designsystem.icons.getCategoryIcon
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.media.ImageResolver.toLocalIconImageModel
import com.aozijx.passly.core.platform.rememberAppIconPainter
import com.aozijx.passly.domain.model.icon.VaultIconInfo

@Composable
fun VaultItemIcon(
    info: VaultIconInfo,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val context = LocalContext.current

    // 1. 预先解析分类兜底图标
    val fallbackIcon = remember(info.iconName, info.category) {
        val resId = info.iconName?.toIntOrNull()
        when {
            resId != null -> VaultIcons.getIconByRes(resId)
            !info.iconName.isNullOrEmpty() -> VaultIcons.getIconByName(info.iconName)
            else -> getCategoryIcon(context, info.category)
        }
    }

    // 加载阶段与成功标记
    var loadStage by remember(info) { mutableIntStateOf(0) }
    var isImageLoaded by remember(info) { mutableStateOf(false) } // 标记顶层图是否加载成功

    val cleanDomain = remember(info.associatedDomain) {
        info.associatedDomain?.let { FaviconUtils.cleanDomain(it) }
    }

    val hasCustom = !info.iconCustomPath.isNullOrBlank()
    val hasDomain = !cleanDomain.isNullOrBlank()
    val hasPackage = !info.associatedAppPackage.isNullOrBlank()

    Box(
        modifier = modifier.size(36.dp), contentAlignment = Alignment.Center
    ) {
        // --- 占位/兜底层 ---
        // 只有当图片未加载成功时，才显示默认图标
        if (!isImageLoaded) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }

        // --- 资源加载层 ---
        if (hasCustom || hasDomain || hasPackage) {
            when (loadStage) {
                0 -> {
                    val model = toLocalIconImageModel(info.iconCustomPath)
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onSuccess = { isImageLoaded = true },
                            onError = { loadStage = 1 })
                    } else {
                        loadStage = 1
                    }
                }

                1 -> {
                    AsyncImage(
                        model = "https://$cleanDomain/favicon.ico",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit,
                        onSuccess = { isImageLoaded = true },
                        onError = { loadStage = 2 })
                }

                2 -> {
                    val iconPainter = info.associatedAppPackage?.let { rememberAppIconPainter(it) }
                    if (iconPainter != null) {
                        // Image 加载通常是同步的或极快的，直接标记成功
                        isImageLoaded = true
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        loadStage = 3
                    }
                }
            }
        }
    }
}
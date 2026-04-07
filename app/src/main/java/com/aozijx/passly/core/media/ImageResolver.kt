package com.aozijx.passly.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 核心媒体解析器：整合路径解析与图片显示优先级逻辑。
 */
object ImageResolver {
    /**
     * 检查路径是否为远程 URL。
     */
    fun isRemoteIconPath(path: String?): Boolean {
        val value = path?.trim().orEmpty()
        return value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
    }

    /**
     * 将路径转换为 Coil 可识别的本地 File 模型。
     */
    fun toLocalIconImageModel(path: String?): String? {
        val value = path?.trim().orEmpty()
        if (value.isEmpty() || isRemoteIconPath(value)) return null
        return if (value.startsWith("file://", ignoreCase = true)) value else "file://$value"
    }

    /**
     * 核心优先级解析：用户自定义图片 > 域名图标 (Favicon) > App 包名图标。
     * 优化：每一级逻辑如果返回空字符串，将自动顺延到下一级。
     * @param allowPackage 是否允许返回包名（背景图场景通常设为 false）。
     */
    fun resolveImageSource(
        customPath: String?,
        domain: String?,
        packageName: String?,
        allowPackage: Boolean = true
    ): Any? {
        // 1. 优先级最高：用户自定义图片
        val customModel = toLocalIconImageModel(customPath)
        if (!customModel.isNullOrBlank()) return customModel

        // 2. 优先级中：域名图标 (Favicon)
        if (!domain.isNullOrBlank()) {
            return domain  // 返回原始 domain，由调用方决定如何获取 favicon
        }

        // 3. 优先级低：App 包名
        if (allowPackage && !packageName.isNullOrBlank()) {
            return packageName
        }

        return null
    }
}

/**
 * UI 层解析结果。
 */
data class ResolvedVaultIcon(
    val model: Any?,
    val isPackage: Boolean
)

@Composable
fun rememberResolvedVaultIcon(
    customPath: String?,
    domain: String?,
    packageName: String?,
    includePackage: Boolean = true
): ResolvedVaultIcon {
    return remember(customPath, domain, packageName, includePackage) {
        val resolved = ImageResolver.resolveImageSource(customPath, domain, packageName, includePackage)
        ResolvedVaultIcon(
            model = resolved,
            isPackage = resolved != null && resolved == packageName
        )
    }
}

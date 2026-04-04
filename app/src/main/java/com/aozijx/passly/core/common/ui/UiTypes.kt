package com.aozijx.passly.core.common.ui


/**
 * 列表卡片样式
 * 用于在设置中切换不同渲染效果。
 */
enum class VaultCardStyle(val key: String, val displayName: String, val description: String) {
    BASE("base", "基础卡片", "统一简洁风格"),
    PASSWORD("password", "密码卡片", "强调凭据识别的样式");

    companion object {
        fun fromKey(key: String?): VaultCardStyle {
            return entries.firstOrNull { it.key == key } ?: BASE
        }
    }
}

package com.aozijx.passly.core.common.ui


/**
 * 列表卡片样式
 * 用于在设置中切换不同渲染效果。
 */
enum class VaultCardStyle(val key: String, val displayName: String, val description: String) {
    DEFAULT("default", "跟随默认", "未单独配置时使用默认样式"),
    BASE("base", "基础卡片", "统一简洁风格"),
    PASSWORD("password", "密码卡片", "强调凭据识别的样式"),
    TOTP("totp", "TOTP卡片", "突出动态验证码与有效期");

    companion object {
        val settingsStyles: List<VaultCardStyle> = listOf(BASE, PASSWORD, TOTP)
        val perTypeStyles: List<VaultCardStyle> = listOf(DEFAULT, BASE, PASSWORD, TOTP)
        val defaultStyle: VaultCardStyle = BASE

        fun fromKey(key: String?): VaultCardStyle {
            return entries.firstOrNull { it.key == key } ?: BASE
        }

        fun resolveSettingsStyle(style: VaultCardStyle): VaultCardStyle {
            return if (style in settingsStyles) style else defaultStyle
        }
    }
}

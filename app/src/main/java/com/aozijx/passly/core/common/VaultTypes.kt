package com.aozijx.passly.core.common

/**
 * 保险箱顶部的分类标签页
 */
enum class VaultTab { 
    ALL,        // 全部
    PASSWORDS,  // 仅显示密码项
    TOTP        // 仅显示 2FA 令牌
}

/**
 * 新增条目的类型枚举
 * 用于控制弹窗路由器 (VaultDialogs) 显示哪种表单
 */
enum class AddType { 
    NONE,       // 无状态
    PASSWORD,   // 密码类型
    TOTP,       // 2FA 类型
    SCAN        // 扫码导入类型
}

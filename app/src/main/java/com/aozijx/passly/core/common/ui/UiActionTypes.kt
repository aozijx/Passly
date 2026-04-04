package com.aozijx.passly.core.common.ui

/**
 * UI 层新增操作类型（Presentation Action）
 *
 * 控制弹窗路由器显示哪种表单，属于 UI 交互层。
 * 注意：这与业务类型(EntryType)是正交的关系
 */
enum class AddType {
    NONE,      // 无活跃操作
    PASSWORD,  // 添加密码
    TOTP,      // 添加 2FA
    SCAN       // 扫码导入
}

/**
 * UI 层图片选择来源类型
 *
 * 用于区分图片选择后的回填目标（头像/封面/截图）。
 */
enum class ImageType {
    AVATAR,
    COVER,
    SCREEN
}


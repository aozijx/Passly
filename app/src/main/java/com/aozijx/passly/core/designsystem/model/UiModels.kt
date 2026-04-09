package com.aozijx.passly.core.designsystem.model

/**
 * UI 层条目筛选器（Presentation State）
 */
enum class VaultTab {
    ALL,        // 全部条目
    PASSWORDS,  // 仅密码类型
    TOTP        // 仅两步验证
}

/**
 * UI 层删除确认状态
 */
enum class DeleteState {
    IDLE,
    CONFIRMING,
    PROCESSING,
    SUCCESS,
    ERROR
}

/**
 * UI 层编辑模式
 */
enum class EditMode {
    VIEW,
    EDIT,
    CREATE
}

/**
 * UI 层新增操作类型
 */
enum class AddType {
    NONE,
    PASSWORD,
    TOTP,
    SCAN
}

/**
 * UI 层图片选择来源类型
 */
enum class ImageType {
    AVATAR,
    COVER,
    SCREEN
}

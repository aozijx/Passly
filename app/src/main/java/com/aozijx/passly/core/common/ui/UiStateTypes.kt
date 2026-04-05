package com.aozijx.passly.core.common.ui

/**
 * UI 层条目筛选器（Presentation State）
 *
 * 用于展示层的标签页切换，与业务逻辑无关。
 * 这是一个纯 UI 问题，独立于条目的业务类型。
 */
enum class VaultTab {
    ALL,        // 全部条目
    PASSWORDS,  // 仅密码类型
    TOTP        // 仅两步验证
}

/**
 * UI 层删除确认状态
 * 用于控制删除对话框的显示
 */
enum class DeleteState {
    IDLE,           // 无活跃删除操作
    CONFIRMING,     // 等待用户确认
    PROCESSING,     // 删除中
    SUCCESS,        // 删除成功
    ERROR           // 删除失败
}

/**
 * UI 层编辑模式
 */
enum class EditMode {
    VIEW,      // 只读模式
    EDIT,      // 编辑模式
    CREATE     // 创建模式
}


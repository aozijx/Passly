package com.aozijx.passly.core.common

/**
 * UI 层条目筛选器（Presentation State）
 *
 * 用于展示层的标签页切换，与业务逻辑无关。
 * 这是一个纯 UI 问题，独立于条目的业务类型。
 */
enum class VaultTab {
    ALL,        // 全部条目
    PASSWORDS,  // 仅密码类型
    TOTP       // 仅两步验证
}

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

/**
 * UI 层删除确认状态
 * 用于控制删除对话框的显示
 */
enum class DeleteState {
    IDLE,           // 无活跃删除操作
    CONFIRMING,     // 等待用户确认
    PROCESSING,     // 删除中
    SUCCESS,        // 删除成功
    ERROR          // 删除失败
}

/**
 * UI 层编辑模式
 */
enum class EditMode {
    VIEW,      // 只读模式
    EDIT,      // 编辑模式
    CREATE     // 创建模式
}

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

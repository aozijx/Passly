// 这里用于存放 Detail feature 的内部实现细节类型，避免暴露给外部模块。


package com.aozijx.passly.ui.features.detail.internal

import com.aozijx.passly.ui.features.detail.page.DetailOpenRequest

// 示例：internal 类型定义
data class VaultDetailCoordinatorState(
    val request: DetailOpenRequest? = null,
    val isIconPickerVisible: Boolean = false
)

// 你可以将更多 internal 相关实现迁移到此处。
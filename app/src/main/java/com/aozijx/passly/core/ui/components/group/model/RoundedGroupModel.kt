package com.aozijx.passly.core.ui.components.group.model

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** 分组内可见项的圆角位置角色。 */
enum class RoundedGroupItemPosition {
    Single,
    First,
    Middle,
    Last
}

/** 单个分组项可用的外观参数（由 [RoundedGroup] 按位置计算后注入）。 */
@Immutable
data class RoundedGroupItemScope(
    val position: RoundedGroupItemPosition,
    val shape: Shape,
    val containerColor: Color,
    val contentPadding: PaddingValues
)

/**
 * 分组项的不可变声明。
 *
 * [key] 必须在同一分组内唯一，并在插入、删除和重新排序时保持不变。
 */
class RoundedGroupItem(
    val key: String,
    val visible: Boolean = true,
    val content: @Composable (RoundedGroupItemScope) -> Unit
)

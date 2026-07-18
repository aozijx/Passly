package com.aozijx.passly.ui.components.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class RoundedGroupItemPosition {
    Single,
    First,
    Middle,
    Last
}

@Immutable
data class RoundedGroupItemScope(
    val position: RoundedGroupItemPosition,
    val shape: Shape,
    val containerColor: Color,
    val contentPadding: PaddingValues
)

/**
 * [key] 必须在同一分组内唯一，并在插入、删除和重新排序时保持不变。
 */
class RoundedGroupItem(
    val key: String,
    val visible: Boolean = true,
    val content: @Composable (RoundedGroupItemScope) -> Unit
)

/**
 * 通用圆角分组。
 *
 * Item 由调用方以不可变列表提供，状态也由调用方持有。组本身只负责可见项位置、
 * 间距和外观计算，不缓存或收集 Composable。可通过 [shapeFactory] 完全替换圆角策略。
 */
@Composable
fun RoundedGroup(
    items: List<RoundedGroupItem>,
    modifier: Modifier = Modifier,
    outerRadius: Dp = 18.dp,
    innerRadius: Dp = 2.dp,
    itemSpacing: Dp = 2.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    shapeFactory: ((RoundedGroupItemPosition) -> Shape)? = null
) {
    val keys = HashSet<String>(items.size)
    var visibleCount = 0
    items.forEach { item ->
        require(keys.add(item.key)) { "RoundedGroup item key must be unique: ${item.key}" }
        if (item.visible) visibleCount++
    }
    var nextVisibleIndex = 0

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            val visibleIndex = if (item.visible) nextVisibleIndex++ else -1
            val position = visibleIndex.toPosition(visibleCount)
            val scope = RoundedGroupItemScope(
                position = position,
                shape = shapeFactory?.invoke(position)
                    ?: position.defaultShape(outerRadius, innerRadius),
                containerColor = containerColor,
                contentPadding = contentPadding
            )

            key(item.key) {
                AnimatedVisibility(
                    visible = item.visible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        item.content(scope)
                        if (visibleIndex >= 0 && visibleIndex < visibleCount - 1) {
                            Spacer(modifier = Modifier.height(itemSpacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    itemScope: RoundedGroupItemScope,
    modifier: Modifier = Modifier,
    shape: Shape = itemScope.shape,
    containerColor: Color = itemScope.containerColor,
    contentPadding: PaddingValues = itemScope.contentPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        enabled = onClick != null,
        onClick = onClick ?: {},
        shape = shape,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

private fun Int.toPosition(visibleCount: Int): RoundedGroupItemPosition = when {
    visibleCount <= 1 -> RoundedGroupItemPosition.Single
    this == 0 -> RoundedGroupItemPosition.First
    this == visibleCount - 1 -> RoundedGroupItemPosition.Last
    else -> RoundedGroupItemPosition.Middle
}

private fun RoundedGroupItemPosition.defaultShape(
    outerRadius: Dp,
    innerRadius: Dp
): RoundedCornerShape = when (this) {
    RoundedGroupItemPosition.Single -> RoundedCornerShape(outerRadius)
    RoundedGroupItemPosition.First -> RoundedCornerShape(
        topStart = outerRadius,
        topEnd = outerRadius,
        bottomStart = innerRadius,
        bottomEnd = innerRadius
    )

    RoundedGroupItemPosition.Middle -> RoundedCornerShape(innerRadius)
    RoundedGroupItemPosition.Last -> RoundedCornerShape(
        topStart = innerRadius,
        topEnd = innerRadius,
        bottomStart = outerRadius,
        bottomEnd = outerRadius
    )
}

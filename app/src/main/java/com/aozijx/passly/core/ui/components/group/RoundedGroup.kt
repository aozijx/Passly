package com.aozijx.passly.core.ui.components.group

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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItemPosition
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItemScope
import com.aozijx.passly.core.ui.theme.PasslyTheme
import com.aozijx.passly.core.ui.theme.RoundedGroupStyle

/**
 * 通用圆角分组。
 *
 * Item 由调用方以不可变列表提供，状态也由调用方持有。组本身只负责可见项位置、
 * 间距和外观计算，不缓存或收集 Composable。默认参数统一读取 [PasslyTheme]，
 * 可通过 [style] 局部覆盖整组参数，或通过 [shapeFactory] 完全替换圆角策略。
 */
@Composable
fun RoundedGroup(
    items: List<RoundedGroupItem>,
    modifier: Modifier = Modifier,
    style: RoundedGroupStyle? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shapeFactory: ((RoundedGroupItemPosition) -> Shape)? = null
) {
    val resolvedStyle = style ?: PasslyTheme.roundedGroup
    val enterTransition =
        fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                expandVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
                )
    val exitTransition =
        fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                shrinkVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
                )
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
                    ?: position.defaultShape(
                        resolvedStyle.outerRadius,
                        resolvedStyle.innerRadius
                    ),
                containerColor = containerColor,
                contentPadding = resolvedStyle.paddingValues
            )

            key(item.key) {
                AnimatedVisibility(
                    visible = item.visible,
                    enter = enterTransition,
                    exit = exitTransition
                ) {
                    Column {
                        item.content(scope)
                        if (visibleIndex >= 0 && visibleIndex < visibleCount - 1) {
                            Spacer(modifier = Modifier.height(resolvedStyle.itemSpacing))
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

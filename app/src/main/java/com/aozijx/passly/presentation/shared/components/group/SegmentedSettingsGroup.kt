package com.aozijx.passly.presentation.shared.components.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.aozijx.passly.presentation.shared.components.group.model.SegmentedSettingsItem

/**
 * Material Expressive 分段列表分组。
 *
 * Item 由调用方以不可变列表提供，状态也由调用方持有。组本身只负责可见项索引、
 * 稳定 key 和显隐动画；形状、间距及交互状态全部交给 Material 3。
 */
@Composable
fun SegmentedSettingsGroup(
    items: List<SegmentedSettingsItem>,
    modifier: Modifier = Modifier,
) {
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
        require(keys.add(item.key)) { "Segmented settings item key must be unique: ${item.key}" }
        if (item.visible) visibleCount++
    }
    var nextVisibleIndex = 0

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            val visibleIndex = if (item.visible) nextVisibleIndex++ else -1
            val shapes =
                ListItemDefaults.segmentedShapes(
                    index = visibleIndex.coerceAtLeast(0),
                    count = visibleCount.coerceAtLeast(1),
                )

            key(item.key) {
                AnimatedVisibility(
                    visible = item.visible,
                    enter = enterTransition,
                    exit = exitTransition
                ) {
                    Column {
                        item.content(shapes)
                        if (visibleIndex >= 0 && visibleIndex < visibleCount - 1) {
                            Spacer(modifier = Modifier.height(ListItemDefaults.SegmentedGap))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun settingsSegmentedColors(
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContainerColor = selectedContainerColor,
)

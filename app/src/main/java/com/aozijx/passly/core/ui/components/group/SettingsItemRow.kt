package com.aozijx.passly.core.ui.components.group

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 设置项行的基础布局：leading + 主内容（可扩展）+ trailing。 */
@Composable
internal fun SettingsItemRow(
    leading: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), content = content)
        trailing()
    }
}

/** 设置值文案的切换动画（值变化时上下滑动过渡）。 */
@Composable
internal fun AnimatedSettingValue(
    value: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) = AnimatedSettingValue(
    targetState = value,
    valueLabel = { it },
    enabled = enabled,
    modifier = modifier
)

/**
 * Animates a setting value between ordered states. [transitionDirection] is evaluated for every
 * interrupted transition, so a rapid target reversal changes direction from the current target.
 */
@Composable
internal fun <T> AnimatedSettingValue(
    targetState: T,
    valueLabel: (T) -> String,
    transitionDirection: (initial: T, target: T) -> Int = { _, _ -> 1 },
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val verticalDirection = transitionDirection(initialState, targetState)
                .coerceIn(-1, 1)
                .takeIf { it != 0 }
                ?: 1
            slideInVertically(
                animationSpec = motionScheme.defaultSpatialSpec()
            ) { it * verticalDirection } togetherWith slideOutVertically(
                animationSpec = motionScheme.defaultSpatialSpec()
            ) { -it * verticalDirection }
        },
        label = "setting_value"
    ) { targetValue ->
        Text(
            text = valueLabel(targetValue),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/** 将 [ImageVector] 图标转换为行首组件；[placeholder] 为 true 时用空白占位对齐。 */
internal fun ImageVector?.asLeadingContent(
    placeholder: Boolean
): (@Composable () -> Unit)? = when {
    this != null -> {
        val image = this
        {
            Icon(
                imageVector = image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }

    placeholder -> {
        { Spacer(modifier = Modifier.width(40.dp)) }
    }

    else -> null
}

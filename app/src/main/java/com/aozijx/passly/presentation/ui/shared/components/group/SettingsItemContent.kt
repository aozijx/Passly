package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 设置值文案的切换动画（值变化时上下滑动过渡）。 */
@Composable
internal fun AnimatedSettingValue(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    modifier: Modifier = Modifier,
    transitionDirection: (initial: T, target: T) -> Int = { _, _ -> 1 },
    enabled: Boolean = true,
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

/** 将 [ImageVector] 图标转换为 Material 列表项的行首内容。 */
internal fun ImageVector?.asLeadingContent(): (@Composable () -> Unit)? = when {
    this != null -> {
        val image = this
        {
            Icon(
                imageVector = image,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    else -> null
}

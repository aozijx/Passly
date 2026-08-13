package com.aozijx.passly.core.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aozijx.passly.data.settings.model.InterfaceStyleConstraints

/**
 * RoundedGroup 的完整视觉参数。外圈圆角用于首项顶部、末项底部和单项四角；
 * 组内圆角用于所有相邻项目的接缝。
 */
@Immutable
data class RoundedGroupStyle(
    val outerRadius: Dp,
    val innerRadius: Dp,
    val itemSpacing: Dp,
    val contentPadding: Dp
) {
    val paddingValues: PaddingValues
        get() = PaddingValues(contentPadding)
}

@Immutable
data class PasslyThemeTokens(
    val roundedGroup: RoundedGroupStyle
)

private val DefaultThemeTokens = PasslyThemeTokens(
    roundedGroup = RoundedGroupStyle(
        outerRadius = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP.dp,
        innerRadius = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP.dp,
        itemSpacing = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP.dp,
        contentPadding = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP.dp
    )
)

internal val LocalPasslyThemeTokens = staticCompositionLocalOf { DefaultThemeTokens }

/** Passly 自有设计令牌的统一读取入口。 */
object PasslyTheme {
    val tokens: PasslyThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalPasslyThemeTokens.current

    val roundedGroup: RoundedGroupStyle
        @Composable
        @ReadOnlyComposable
        get() = tokens.roundedGroup
}

internal data class PasslyThemeDefinition(
    val shapes: Shapes,
    val tokens: PasslyThemeTokens
)

internal fun passlyThemeDefinition(
    outerCornerRadiusDp: Float,
    innerCornerRadiusDp: Float,
    groupItemSpacingDp: Float,
    groupContentPaddingDp: Float
): PasslyThemeDefinition {
    val outerRadius = outerCornerRadiusDp.coerceIn(
        InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP,
        InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP
    )
    val innerRadius = innerCornerRadiusDp.coerceIn(
        InterfaceStyleConstraints.MIN_INNER_RADIUS_DP,
        InterfaceStyleConstraints.MAX_INNER_RADIUS_DP
    )
    val itemSpacing = groupItemSpacingDp.coerceIn(
        InterfaceStyleConstraints.MIN_ITEM_SPACING_DP,
        InterfaceStyleConstraints.MAX_ITEM_SPACING_DP
    )
    val contentPadding = groupContentPaddingDp.coerceIn(
        InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP,
        InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP
    )

    return PasslyThemeDefinition(
        shapes = Shapes(
            extraSmall = RoundedCornerShape((outerRadius * 0.25f).dp),
            small = RoundedCornerShape((outerRadius * 0.4f).dp),
            medium = RoundedCornerShape((outerRadius * 0.6f).dp),
            large = RoundedCornerShape((outerRadius * 0.8f).dp),
            extraLarge = RoundedCornerShape(outerRadius.dp),
            largeIncreased = RoundedCornerShape(
                (outerRadius * 1.25f)
                    .coerceAtMost(InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP)
                    .dp
            )
        ),
        tokens = PasslyThemeTokens(
            roundedGroup = RoundedGroupStyle(
                outerRadius = outerRadius.dp,
                innerRadius = innerRadius.dp,
                itemSpacing = itemSpacing.dp,
                contentPadding = contentPadding.dp
            )
        )
    )
}

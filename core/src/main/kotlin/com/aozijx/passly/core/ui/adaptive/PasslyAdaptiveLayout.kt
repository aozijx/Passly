package com.aozijx.passly.core.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass

enum class PasslyWindowWidth {
    COMPACT, MEDIUM, EXPANDED
}

@Immutable
data class PasslyAdaptiveLayout(
    val windowWidth: PasslyWindowWidth
) {
    val isExpanded: Boolean
        get() = windowWidth == PasslyWindowWidth.EXPANDED

    val isAtLeastMedium: Boolean
        get() = windowWidth != PasslyWindowWidth.COMPACT

    companion object {
        val Compact = PasslyAdaptiveLayout(PasslyWindowWidth.COMPACT)
    }
}

val LocalPasslyAdaptiveLayout = staticCompositionLocalOf { PasslyAdaptiveLayout.Compact }

/**
 * 在应用根节点集中计算窗口宽度级别，子界面只消费布局决策。
 */
@Composable
fun ProvidePasslyAdaptiveLayout(content: @Composable () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val windowWidth = when {
        windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        ) -> PasslyWindowWidth.EXPANDED

        windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        ) -> PasslyWindowWidth.MEDIUM

        else -> PasslyWindowWidth.COMPACT
    }
    val layout = remember(windowWidth) { PasslyAdaptiveLayout(windowWidth) }

    CompositionLocalProvider(LocalPasslyAdaptiveLayout provides layout, content = content)
}

package com.aozijx.passly.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class TopBarState {
    var title by mutableStateOf("")
    var actions by mutableStateOf<@Composable RowScope.() -> Unit>({})
    var navigationIcon by mutableStateOf<@Composable (() -> Unit)?>(null)
    var isVisible by mutableStateOf(true)
    var centerTitle by mutableStateOf(false)

    fun update(
        title: String = "",
        actions: @Composable RowScope.() -> Unit = {},
        navigationIcon: @Composable (() -> Unit)? = null,
        isVisible: Boolean = true,
        centerTitle: Boolean = false
    ) {
        this.title = title
        this.actions = actions
        this.navigationIcon = navigationIcon
        this.isVisible = isVisible
        this.centerTitle = centerTitle
    }

    fun reset() {
        this.title = ""
        this.actions = {}
        this.navigationIcon = null
        this.isVisible = true
        this.centerTitle = false
    }
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

/**
 * 优雅方案：生命周期自动清理
 * 使用 DisposableEffect 确保当页面退出组合（Dispose）时，TopBar 状态会自动重置。
 */
@Composable
fun TopBarConfig(
    title: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null,
    isVisible: Boolean = true,
    centerTitle: Boolean = false
) {
    val state = LocalTopBarState.current

    DisposableEffect(title, actions, navigationIcon, isVisible, centerTitle) {
        state.update(
            title = title,
            actions = actions,
            navigationIcon = navigationIcon,
            isVisible = isVisible,
            centerTitle = centerTitle
        )
        onDispose {
            state.reset()
        }
    }
}
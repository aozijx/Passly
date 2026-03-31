package com.example.poop.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
}

val LocalTopBarState = staticCompositionLocalOf { TopBarState() }

@Composable
fun TopBarConfig(
    title: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null,
    isVisible: Boolean = true,
    centerTitle: Boolean = false
) {
    val state = LocalTopBarState.current
    SideEffect {
        state.update(title, actions, navigationIcon, isVisible, centerTitle)
    }
}

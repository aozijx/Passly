package com.aozijx.passly.presentation.ui.vault.list.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Composable
internal fun rememberFabVisibilityNestedScrollConnection(
    onVisibilityChanged: (Boolean) -> Unit,
): NestedScrollConnection {
    val latestOnVisibilityChanged = rememberUpdatedState(onVisibilityChanged)
    val gestureState = remember {
        FabVisibilityGestureState { isVisible ->
            latestOnVisibilityChanged.value(isVisible)
        }
    }
    return remember(gestureState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                gestureState.onScroll(available.y)
                return Offset.Zero
            }
        }
    }
}

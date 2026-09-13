package com.aozijx.passly.presentation.ui.vault.list.gesture

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberPullToSearchNestedScrollConnection(
    gridState: LazyGridState,
    enabled: Boolean,
    onProgressChanged: (Float) -> Unit,
    onTriggered: () -> Unit,
): NestedScrollConnection {
    val thresholdPx = with(LocalDensity.current) { PullToSearchThreshold.toPx() }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val currentOnTriggered by rememberUpdatedState(onTriggered)
    val gestureState = remember(thresholdPx) {
        PullToSearchGestureState(
            thresholdPx = thresholdPx,
            onProgressChanged = { currentOnProgressChanged(it) },
            onTriggered = { currentOnTriggered() },
        )
    }

    return remember(gridState, gestureState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (currentEnabled && source == NestedScrollSource.UserInput) {
                    gestureState.onPull(
                        deltaY = available.y,
                        isAtTop = !gridState.canScrollBackward,
                    )
                } else {
                    gestureState.reset()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                gestureState.reset()
                return Velocity.Zero
            }
        }
    }
}

private val PullToSearchThreshold = 72.dp

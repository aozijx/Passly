package com.aozijx.passly.feature.vault.presentation.list.ui.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Keeps one nested-scroll concern ahead of another while preserving the remaining delta.
 * Pull-to-search uses this to retract its active pull before the app bar starts collapsing.
 */
internal class PrioritizedNestedScrollConnection(
    private val priority: NestedScrollConnection,
    private val secondary: NestedScrollConnection,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val priorityConsumed = priority.onPreScroll(available, source)
        val secondaryConsumed = secondary.onPreScroll(available - priorityConsumed, source)
        return priorityConsumed + secondaryConsumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val priorityConsumed = priority.onPostScroll(consumed, available, source)
        val secondaryConsumed = secondary.onPostScroll(
            consumed = consumed + priorityConsumed,
            available = available - priorityConsumed,
            source = source,
        )
        return priorityConsumed + secondaryConsumed
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val priorityConsumed = priority.onPreFling(available)
        val secondaryConsumed = secondary.onPreFling(available - priorityConsumed)
        return priorityConsumed + secondaryConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val priorityConsumed = priority.onPostFling(consumed, available)
        val secondaryConsumed = secondary.onPostFling(
            consumed = consumed + priorityConsumed,
            available = available - priorityConsumed,
        )
        return priorityConsumed + secondaryConsumed
    }
}

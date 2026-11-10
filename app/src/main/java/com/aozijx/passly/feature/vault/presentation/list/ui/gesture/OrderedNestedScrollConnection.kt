package com.aozijx.passly.feature.vault.presentation.list.ui.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** Dispatches each nested-scroll phase in order, passing each connection only the unconsumed delta. */
internal class OrderedNestedScrollConnection(connections: List<NestedScrollConnection>) : NestedScrollConnection {
    private val connections = connections.toList()

    init {
        require(this.connections.size > 1) { "A connection chain requires at least two connections." }
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        var consumed = Offset.Zero
        var remaining = available
        connections.forEach { connection ->
            val nextConsumed = connection.onPreScroll(remaining, source)
            consumed += nextConsumed
            remaining -= nextConsumed
        }
        return consumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        var consumedByConnections = Offset.Zero
        connections.forEach { connection ->
            val consumedSoFar = consumed + consumedByConnections
            val remaining = available - consumedByConnections
            val nextConsumed = connection.onPostScroll(consumedSoFar, remaining, source)
            consumedByConnections += nextConsumed
        }
        return consumedByConnections
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        var consumed = Velocity.Zero
        var remaining = available
        connections.forEach { connection ->
            val nextConsumed = connection.onPreFling(remaining)
            consumed += nextConsumed
            remaining -= nextConsumed
        }
        return consumed
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        var consumedByConnections = Velocity.Zero
        connections.forEach { connection ->
            val consumedSoFar = consumed + consumedByConnections
            val remaining = available - consumedByConnections
            val nextConsumed = connection.onPostFling(consumedSoFar, remaining)
            consumedByConnections += nextConsumed
        }
        return consumedByConnections
    }
}

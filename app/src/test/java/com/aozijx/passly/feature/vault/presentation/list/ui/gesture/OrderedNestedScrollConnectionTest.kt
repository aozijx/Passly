package com.aozijx.passly.feature.vault.presentation.list.ui.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderedNestedScrollConnectionTest {
    @Test
    fun `reverse pull is consumed before the app bar receives remaining scroll`() {
        val receivedByAppBar = mutableListOf<Offset>()
        val pullToSearch =
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = Offset(x = 0f, y = -30f)
            }
        val appBar =
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    receivedByAppBar += available
                    return available
                }
            }
        val connection =
            OrderedNestedScrollConnection(listOf(pullToSearch, appBar))

        val consumed =
            connection.onPreScroll(
                available = Offset(x = 0f, y = -50f),
                source = NestedScrollSource.UserInput,
            )

        assertEquals(listOf(Offset(x = 0f, y = -20f)), receivedByAppBar)
        assertEquals(Offset(x = 0f, y = -50f), consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `post scroll and fling preserve priority and remaining deltas`() = runTest {
        var secondaryPostScroll: Pair<Offset, Offset>? = null
        var secondaryPreFling: Velocity? = null
        var secondaryPostFling: Pair<Velocity, Velocity>? = null
        val priority =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = Offset(x = 0f, y = -5f)

                override suspend fun onPreFling(available: Velocity): Velocity = Velocity(x = 0f, y = -2f)

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity = Velocity(x = 0f, y = -3f)
            }
        val secondary =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    secondaryPostScroll = consumed to available
                    return available
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    secondaryPreFling = available
                    return available
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    secondaryPostFling = consumed to available
                    return available
                }
            }
        val connection = OrderedNestedScrollConnection(listOf(priority, secondary))

        val postScrollConsumed =
            connection.onPostScroll(
                consumed = Offset(x = 0f, y = -10f),
                available = Offset(x = 0f, y = -20f),
                source = NestedScrollSource.UserInput,
            )
        val preFlingConsumed = connection.onPreFling(Velocity(x = 0f, y = -10f))
        val postFlingConsumed =
            connection.onPostFling(
                consumed = Velocity(x = 0f, y = 10f),
                available = Velocity(x = 0f, y = 20f),
            )

        assertEquals(Offset(x = 0f, y = -15f) to Offset(x = 0f, y = -15f), secondaryPostScroll)
        assertEquals(Offset(x = 0f, y = -20f), postScrollConsumed)
        assertEquals(Velocity(x = 0f, y = -8f), secondaryPreFling)
        assertEquals(Velocity(x = 0f, y = -10f), preFlingConsumed)
        assertEquals(
            Velocity(x = 0f, y = 7f) to Velocity(x = 0f, y = 23f),
            secondaryPostFling,
        )
        assertEquals(Velocity(x = 0f, y = 20f), postFlingConsumed)
    }
}

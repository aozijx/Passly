package com.aozijx.passly.feature.vault.presentation.list.ui.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

class PrioritizedNestedScrollConnectionTest {
    @Test
    fun `reverse pull is consumed before the app bar receives remaining scroll`() {
        val receivedByAppBar = mutableListOf<Offset>()
        val pullToSearch = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                Offset(x = 0f, y = -30f)
        }
        val appBar = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                receivedByAppBar += available
                return available
            }
        }
        val connection = PrioritizedNestedScrollConnection(
            priority = pullToSearch,
            secondary = appBar,
        )

        val consumed = connection.onPreScroll(
            available = Offset(x = 0f, y = -50f),
            source = NestedScrollSource.UserInput,
        )

        assertEquals(listOf(Offset(x = 0f, y = -20f)), receivedByAppBar)
        assertEquals(Offset(x = 0f, y = -50f), consumed)
    }
}

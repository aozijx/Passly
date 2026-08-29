package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultListEventDispatcherTest {
    @Test
    fun `updated callbacks receive events and authentication without replacing dispatcher`() {
        val dispatcher = VaultListEventDispatcher()
        val events = mutableListOf<String>()

        dispatcher.updateCallbacks(
            onEvent = { events += "old:$it" },
            requestAuthentication = { events += "old-auth"; it() },
        )
        dispatcher.updateCallbacks(
            onEvent = { events += "new:$it" },
            requestAuthentication = { events += "new-auth"; it() },
        )

        dispatcher.onEvent(VaultListEvent.SettingsClicked)
        dispatcher.requestAuthentication { events += "authenticated" }

        assertEquals(
            listOf("new:SettingsClicked", "new-auth", "authenticated"),
            events,
        )
    }
}

package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultOtpStateProviderTest {
    @Test
    fun `provider maps current code and dispatches latest visibility callbacks`() = runBlocking {
        val states = MutableStateFlow(
            mapOf("otp-1" to OtpCodeState.Ready(code = "123456", progress = 0.5f)),
        )
        val provider = VaultOtpStateProviderBindings(states)
        val events = mutableListOf<String>()

        provider.updateSubscriptions(
            onSubscribe = { events += "old-subscribe:$it" },
            onUnsubscribe = { events += "old-unsubscribe:$it" },
        )
        provider.updateSubscriptions(
            onSubscribe = { events += "subscribe:$it" },
            onUnsubscribe = { events += "unsubscribe:$it" },
        )

        provider.subscribe("otp-1")
        val state = provider.state("otp-1").first()
        require(state is VaultOtpUiState.Ready)
        assertEquals("123456", state.code)
        provider.unsubscribe("otp-1")

        assertEquals(listOf("subscribe:otp-1", "unsubscribe:otp-1"), events)
    }
}

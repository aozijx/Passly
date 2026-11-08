package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultOtpStateProviderTest {
    @Test
    fun `provider maps code states and dispatches latest visibility callbacks`() =
        runBlocking {
            val states =
                MutableStateFlow(
                    mapOf(
                        "pending" to OtpCodeState.Pending,
                        "otp-1" to OtpCodeState.Ready(code = "123456", progress = 0.5f),
                        "failed" to OtpCodeState.Failed(OtpGenerationError.InvalidSecret),
                    ),
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
            val state = provider.state("otp-1", VaultOtpKindUiModel.STANDARD).first()
            assertEquals(VaultOtpUiState.Ready(displayCode = "123 456", progress = 0.5f), state)
            assertEquals(VaultOtpUiState.Pending, provider.state("pending", null).first())
            assertEquals(VaultOtpUiState.Failed, provider.state("failed", null).first())
            assertEquals(null, provider.state("missing", null).first())
            provider.unsubscribe("otp-1")

            assertEquals(listOf("subscribe:otp-1", "unsubscribe:otp-1"), events)
        }
}

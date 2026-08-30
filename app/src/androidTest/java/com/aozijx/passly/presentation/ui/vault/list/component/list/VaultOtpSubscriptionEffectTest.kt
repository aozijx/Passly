package com.aozijx.passly.presentation.ui.vault.list.component.list

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultOtpSubscriptionEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun otpCompositionSubscribesAndDisposalUnsubscribes() {
        val showEntry = mutableStateOf(true)
        val events = mutableListOf<String>()
        val provider = object : VaultOtpStateProvider {
            override fun state(entryId: String): Flow<VaultOtpUiState?> =
                flowOf(VaultOtpUiState(code = "123456"))

            override fun subscribe(entryId: String) {
                events += "subscribe:$entryId"
            }

            override fun unsubscribe(entryId: String) {
                events += "unsubscribe:$entryId"
            }
        }

        composeRule.setContent {
            if (showEntry.value) observeVaultOtpState("otp-1", provider, enabled = true)
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-1"), events)
            showEntry.value = false
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-1", "unsubscribe:otp-1"), events)
        }
    }

    @Test
    fun otpSubscriptionFollowsCodeVisibilityAndActivePage() {
        val enabled = mutableStateOf(false)
        val events = mutableListOf<String>()
        val provider = object : VaultOtpStateProvider {
            override fun state(entryId: String): Flow<VaultOtpUiState?> = flowOf(null)

            override fun subscribe(entryId: String) {
                events += "subscribe:$entryId"
            }

            override fun unsubscribe(entryId: String) {
                events += "unsubscribe:$entryId"
            }
        }

        composeRule.setContent {
            observeVaultOtpState("otp-2", provider, enabled = enabled.value)
        }
        composeRule.runOnIdle {
            assertEquals(emptyList<String>(), events)
            enabled.value = true
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-2"), events)
            enabled.value = false
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-2", "unsubscribe:otp-2"), events)
        }
    }
}

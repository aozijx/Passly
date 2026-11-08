package com.aozijx.passly.feature.vault.presentation.list.ui.component.list

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.feature.vault.presentation.list.VaultOtpStateProvider
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultOtpSubscriptionEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun subscriptionTracksVisibilityAndCompositionLifetime() {
        val showEntry = mutableStateOf(true)
        val enabled = mutableStateOf(false)
        val events = mutableListOf<String>()
        val provider =
            object : VaultOtpStateProvider {
                override fun state(
                    entryId: String,
                    otpKind: VaultOtpKindUiModel?,
                ): Flow<VaultOtpUiState?> =
                    flowOf(VaultOtpUiState.Ready(displayCode = "123456", progress = 0.5f))

                override fun subscribe(entryId: String) {
                    events += "subscribe:$entryId"
                }

                override fun unsubscribe(entryId: String) {
                    events += "unsubscribe:$entryId"
                }
            }

        composeRule.setContent {
            if (showEntry.value) {
                observeVaultOtpState(
                    entryId = "otp-1",
                    otpKind = VaultOtpKindUiModel.STANDARD,
                    provider = provider,
                    enabled = enabled.value,
                )
            }
        }
        composeRule.runOnIdle {
            assertEquals(emptyList<String>(), events)
            enabled.value = true
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-1"), events)
            enabled.value = false
        }
        composeRule.runOnIdle {
            assertEquals(listOf("subscribe:otp-1", "unsubscribe:otp-1"), events)
            enabled.value = true
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf("subscribe:otp-1", "unsubscribe:otp-1", "subscribe:otp-1"),
                events,
            )
            showEntry.value = false
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    "subscribe:otp-1",
                    "unsubscribe:otp-1",
                    "subscribe:otp-1",
                    "unsubscribe:otp-1",
                ),
                events,
            )
        }
    }
}

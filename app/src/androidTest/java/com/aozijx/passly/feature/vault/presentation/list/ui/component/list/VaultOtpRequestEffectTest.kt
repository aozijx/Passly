package com.aozijx.passly.feature.vault.presentation.list.ui.component.list

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.feature.vault.otp.OtpLease
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultOtpRequestEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun visibleOtpEntryOwnsAnOtpLeaseUntilItLeavesComposition() {
        val showEntry = mutableStateOf(true)
        val enabled = mutableStateOf(false)
        val events = mutableListOf<String>()
        val state = MutableStateFlow<OtpUiState>(OtpUiState.NotRequested)
        val stateFlow: (String, OtpPresentation, Boolean) -> StateFlow<OtpUiState> =
            { _, _, _ -> state }
        composeRule.setContent {
            if (showEntry.value) {
                observeVaultOtpState(
                    entryId = "otp-1",
                    otpPresentation = OtpPresentation.Applicable(OtpType.TOTP),
                    otpUiStateFlow = stateFlow,
                    showCode = enabled.value,
                    onAcquireOtp = { entryId ->
                        events += "acquire:$entryId"
                        OtpLease { events += "release:$entryId" }
                    },
                )
            }
        }
        composeRule.runOnIdle {
            assertEquals(emptyList<String>(), events)
            enabled.value = true
        }
        composeRule.runOnIdle {
            assertEquals(listOf("acquire:otp-1"), events)
            enabled.value = false
        }
        composeRule.runOnIdle {
            assertEquals(listOf("acquire:otp-1", "release:otp-1"), events)
            enabled.value = true
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf("acquire:otp-1", "release:otp-1", "acquire:otp-1"),
                events,
            )
            showEntry.value = false
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf("acquire:otp-1", "release:otp-1", "acquire:otp-1", "release:otp-1"),
                events,
            )
        }
    }
}

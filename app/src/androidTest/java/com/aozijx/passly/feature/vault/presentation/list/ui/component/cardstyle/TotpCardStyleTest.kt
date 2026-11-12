package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.presentation.list.preview.CardStylePreviewFixtures
import org.junit.Rule
import org.junit.Test

class TotpCardStyleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun timedCodeCardDisplaysCurrentAndNextCodeWithoutTypeBadge() {
        composeRule.setContent {
            MaterialTheme {
                TotpStyleVaultItem(
                    entry = CardStylePreviewFixtures.totpEntry(EntryType.OTP),
                    totpState =
                        OtpUiState.TimedReady(
                            currentCode = "123 456",
                            nextCode = "654 321",
                            windowEndMillis = 30_000L,
                            periodSeconds = 30,
                        ),
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("123 456").assertIsDisplayed()
        composeRule.onNodeWithText("654 321").assertIsDisplayed()
        composeRule.onAllNodesWithText("TOTP").assertCountEquals(0)
    }
}

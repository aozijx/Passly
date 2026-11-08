package com.aozijx.passly.feature.settings.presentation.ui.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupDirectoryClearDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clearingDisablesConfirmAndDismissActions() {
        var confirmations = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                BackupDirectoryClearDialog(
                    isClearing = true,
                    onConfirm = { confirmations++ },
                    onDismiss = { dismissals++ },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.settings_clear_action)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.settings_clear_action)).performClick()

        assertEquals(0, confirmations)
        assertEquals(0, dismissals)
    }

    @Test
    fun confirmationRunsOnlyAfterUserConfirms() {
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                BackupDirectoryClearDialog(
                    isClearing = false,
                    onConfirm = { confirmations++ },
                    onDismiss = {},
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.settings_clear_action)).performClick()

        assertEquals(1, confirmations)
    }
}

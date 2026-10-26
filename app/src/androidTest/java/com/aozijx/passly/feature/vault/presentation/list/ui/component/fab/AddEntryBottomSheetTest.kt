package com.aozijx.passly.feature.vault.presentation.list.ui.component.fab

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.labelRes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddEntryBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onlySuppliedCreationOptionsAreRenderedAndSelectionIsForwarded() {
        val options = listOf(VaultAddTypeUiModel.BANK_CARD, VaultAddTypeUiModel.WIFI)
        val selected = mutableListOf<VaultAddTypeUiModel>()
        var dismissals = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            MaterialTheme {
                AddEntryBottomSheet(options = options, onDismiss = { dismissals++ }, onSelectType = selected::add)
            }
        }
        for (type in VaultAddTypeUiModel.entries) {
            val node = composeRule.onNodeWithText(context.getString(type.labelRes))
            if (type in options) node.assertIsDisplayed() else node.assertDoesNotExist()
        }
        composeRule.onNodeWithText(context.getString(VaultAddTypeUiModel.WIFI.labelRes)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(VaultAddTypeUiModel.WIFI), selected)
            assertEquals(1, dismissals)
        }
    }
}

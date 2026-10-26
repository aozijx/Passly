package com.aozijx.passly.feature.vault.presentation.editor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.AddBankCardUiState
import com.aozijx.passly.feature.vault.presentation.editor.ui.bankcard.AddBankCardEditorScreen
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.SensitiveFieldVisibilityButton
import org.junit.Rule
import org.junit.Test

class SensitiveFieldVisibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toggleAnnouncesFieldAndVisibilityState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.getString(R.string.card_cvv)
        compose.setContent {
            var visible by remember { mutableStateOf(false) }
            SensitiveFieldVisibilityButton(
                fieldLabelRes = R.string.card_cvv, visible = visible, onVisibilityChanged = { visible = it },
            )
        }
        compose.onNodeWithContentDescription(context.getString(R.string.vault_editor_show_field, label))
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                context.getString(R.string.vault_editor_field_hidden)))
            .performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.vault_editor_hide_field, label))
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                context.getString(R.string.vault_editor_field_visible)))
    }

    @Test
    fun bankCardVisibilityActionsHaveDistinctFieldNames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            AddBankCardEditorScreen(
                state = AddBankCardUiState(), onAction = {}, onBack = {},
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
        for (field in listOf(R.string.card_number, R.string.payment_pin, R.string.card_cvv)) {
            compose.onNodeWithContentDescription(context.getString(
                R.string.vault_editor_show_field, context.getString(field),
            )).assertIsOff()
        }
    }
}

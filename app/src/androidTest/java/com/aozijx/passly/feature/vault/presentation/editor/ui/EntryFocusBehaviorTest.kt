package com.aozijx.passly.feature.vault.presentation.editor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.platform.testTag
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordUiState
import com.aozijx.passly.feature.vault.presentation.editor.ui.password.AddPasswordEditorScreen
import org.junit.Rule
import org.junit.Test

class EntryFocusBehaviorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusMovesThroughFieldsOnNextImeAction() {
        val state = AddPasswordUiState(
            form = AddPasswordFormState(title = "Example", password = "secret"),
        )

        composeRule.setContent {
            AddPasswordEditorScreen(
                state = state,
                onAction = {},
                onBack = {},
                snackbarHostState = remember { SnackbarHostState() },
                modifier = Modifier,
            )
        }

        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val title = resources.getString(R.string.field_title)
        val username = resources.getString(R.string.field_username_hint)
        val password = resources.getString(R.string.password_label)
        composeRule.onNodeWithText(title).performClick().assertIsFocused()
        composeRule.onNodeWithText(title).performImeAction()
        composeRule.onNodeWithText(username).assertIsFocused()
        composeRule.onNodeWithText(username).performImeAction()
        composeRule.onNodeWithText(password).assertIsFocused()
    }

    @Test
    fun screenAndSaveActionModifiersHaveIndependentOwners() {
        val state = AddPasswordUiState(
            form = AddPasswordFormState(title = "Example", password = "secret"),
        )

        composeRule.setContent {
            AddPasswordEditorScreen(
                state = state,
                onAction = {},
                onBack = {},
                snackbarHostState = remember { SnackbarHostState() },
                modifier = Modifier.testTag("editor-screen"),
                saveActionModifier = Modifier.testTag("editor-save-action"),
            )
        }

        composeRule.onNodeWithTag("editor-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("editor-save-action").assertIsDisplayed()
    }
}

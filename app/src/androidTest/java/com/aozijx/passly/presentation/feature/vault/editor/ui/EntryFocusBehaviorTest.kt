package com.aozijx.passly.presentation.feature.vault.editor.ui

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
import com.aozijx.passly.presentation.feature.vault.editor.password.AddPasswordUiState
import com.aozijx.passly.presentation.feature.vault.editor.ui.password.AddPasswordEditorScreen
import org.junit.Rule
import org.junit.Test

class EntryFocusBehaviorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusMovesThroughFieldsOnNextImeAction() {
        val state = AddPasswordUiState(
            canSave = true,
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

        // Start focus on Title
        composeRule.onNodeWithText("标题").performClick().assertIsFocused()
        
        // Move to Username (Next)
        composeRule.onNodeWithText("标题").performImeAction()
        composeRule.onNodeWithText("账号或邮箱").assertIsFocused()

        // Move to Password (Next)
        composeRule.onNodeWithText("账号或邮箱").performImeAction()
        composeRule.onNodeWithText("密码").assertIsFocused()
    }

    @Test
    fun screenAndSaveActionModifiersHaveIndependentOwners() {
        val state = AddPasswordUiState(
            canSave = true,
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

package com.aozijx.passly.presentation.ui.vault.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import com.aozijx.passly.presentation.ui.vault.editor.password.AddPasswordEditorScreen
import com.aozijx.passly.presentation.ui.vault.editor.password.PasswordEditorEventHandler
import com.aozijx.passly.presentation.ui.vault.editor.password.PasswordEditorState
import org.junit.Rule
import org.junit.Test

class EntryFocusBehaviorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun focusMovesThroughFieldsOnNextImeAction() {
        val state = PasswordEditorState(
            title = "",
            username = "",
            password = "",
            website = "",
            notes = "",
            tags = "",
            isPasswordVisible = false,
            isFormValid = true,
            canSave = true,
            isSaving = false
        )
        val eventHandler = PasswordEditorEventHandler(
            onBack = {},
            onSave = {},
            onTitleChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onPasswordVisibilityChange = {},
            onWebsiteChange = {},
            onNotesChange = {},
            onTagsChange = {}
        )

        composeRule.setContent {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    AddPasswordEditorScreen(
                        state = state,
                        onEvent = eventHandler,
                        snackbarHostState = remember { SnackbarHostState() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
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
}

package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailTagEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.TagEditorOverlayUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagEditorSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveDisablesAllDraftControlsAndFinishingRestoresEditing() {
        val saving = mutableStateOf(true)
        val changes = mutableListOf<String>()
        composeRule.setContent {
            MaterialTheme {
                TagEditorSheet(
                    state = DetailTagEditorUiModel.Visible(
                        initialTags = setOf("Work"), draftTags = setOf("Work"),
                        availableTags = setOf("Finance"), input = "", suggestions = listOf("Finance"),
                        validationError = null, overlay = TagEditorOverlayUiState.None,
                    ),
                    isSaving = saving.value,
                    onInputChanged = { changes += it },
                    onSubmit = { changes += "submit:$it" },
                    onRemove = { changes += "remove:$it" },
                    onSave = { changes += "save" },
                    onDismiss = { changes += "dismiss" },
                    onConfirmDiscard = { changes += "discard" },
                    onKeepEditing = { },
                )
            }
        }
        composeRule.onNodeWithTag("tag-editor-input").assertIsNotEnabled()
        composeRule.onNodeWithText("Work").assertIsNotEnabled()
        composeRule.onNodeWithText("Finance").assertIsNotEnabled()
        composeRule.runOnIdle { saving.value = false }
        composeRule.onNodeWithText("Work").assertIsEnabled()
        composeRule.onNodeWithText("Finance").assertIsEnabled()
        composeRule.onNodeWithTag("tag-editor-input").assertIsEnabled().performTextInput("Personal")
        composeRule.runOnIdle { assertEquals(listOf("Personal"), changes) }
    }
}

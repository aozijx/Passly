package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.TagEditorValidationErrorUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagEditorPresentationTest {
    @Test
    fun `tag editor state maps to ui only at the presentation boundary`() {
        val state = DetailTagEditorState(
            visible = true,
            initialTags = setOf("Work"),
            draftTags = setOf("Personal"),
            validationError = DetailTagValidationError.TOO_MANY_TAGS,
        )

        val actual = state.toTagEditorUiModel()

        assertTrue(actual.visible)
        assertTrue(actual.dirty)
        assertEquals(setOf("Personal"), actual.draftTags)
        assertEquals(TagEditorValidationErrorUiModel.TOO_MANY_TAGS, actual.validationError)
    }
}

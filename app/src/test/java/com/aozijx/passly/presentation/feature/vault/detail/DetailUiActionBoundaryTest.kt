package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiActionBoundaryTest {

    @Test
    fun `tag actions expose their capability boundary`() {
        val actions: List<DetailUiAction> = listOf(
            DetailUiAction.OpenTagEditor,
            DetailUiAction.UpdateTagInput("tag"),
            DetailUiAction.SubmitTag("tag"),
            DetailUiAction.RemoveTag("tag"),
            DetailUiAction.SaveTags,
            DetailUiAction.DismissTagEditor,
            DetailUiAction.ConfirmDiscardTags,
            DetailUiAction.KeepEditingTags,
        )

        assertTrue(actions.all { it is DetailTagAction })
    }
}

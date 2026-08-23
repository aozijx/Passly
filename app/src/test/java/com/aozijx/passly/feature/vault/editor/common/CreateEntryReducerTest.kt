package com.aozijx.passly.presentation.feature.vault.editor.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEntryReducerTest {

    @Test
    fun `form change publishes the precomputed validation result`() {
        val result = CreateEntryReducer.reduce(
            CreateEntryUiState(form = "", canSave = false),
            CreateEntryMutation.FormChanged(form = "valid", canSave = true),
        )

        assertEquals("valid", result.form)
        assertTrue(result.canSave)
        assertFalse(result.isSaving)
    }

    @Test
    fun `save start prevents duplicate saves`() {
        val result = CreateEntryReducer.reduce(
            CreateEntryUiState(form = "valid", canSave = true),
            CreateEntryMutation.SaveStarted,
        )

        assertTrue(result.isSaving)
        assertFalse(result.canSave)
    }

    @Test
    fun `failed save restores validation without changing form`() {
        val result = CreateEntryReducer.reduce(
            CreateEntryUiState(form = "valid", canSave = false, isSaving = true),
            CreateEntryMutation.SaveFailed(canSave = true),
        )

        assertEquals("valid", result.form)
        assertFalse(result.isSaving)
        assertTrue(result.canSave)
    }
}

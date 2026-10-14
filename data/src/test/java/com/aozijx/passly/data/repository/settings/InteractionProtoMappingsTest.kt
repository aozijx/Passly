package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.AutofillPreferences
import com.aozijx.passly.domain.settings.model.SwipeActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionProtoMappingsTest {
    @Test
    fun `disabled swipe direction has a stable storage key`() {
        assertEquals("disabled", SwipeActionType.DISABLED.toSwipeActionString())
        assertEquals(SwipeActionType.DISABLED, "disabled".toSwipeActionDomain())
    }

    @Test
    fun `autofill save prompts default to enabled`() {
        val preferences = AutofillPreferences.getDefaultInstance()

        assertTrue(preferences.savePromptsEnabled)
        assertTrue(readAutofill(preferences).savePromptsEnabled)
    }

    @Test
    fun `autofill save prompt value maps independently from credential manager`() {
        val combinations = listOf(
            false to false,
            false to true,
            true to false,
            true to true,
        )

        combinations.forEach { (savePromptsEnabled, credentialManagerEnabled) ->
            val preferences = AutofillPreferences.newBuilder()
                .setSavePromptsEnabled(savePromptsEnabled)
                .setCredentialManagerEnabled(credentialManagerEnabled)
                .build()

            val settings = readAutofill(preferences)

            assertEquals(savePromptsEnabled, settings.savePromptsEnabled)
            assertEquals(credentialManagerEnabled, settings.credentialManagerEnabled)
        }
    }
}

package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyAutofillSaveFlowTest {
    @Test
    fun `save associates only with a matching save enabled legacy interaction`() = runBlocking {
        val store = AutofillInteractionStore()
        val scope = AutofillGrantContext("com.example.app", "https://example.com/login")
        val interaction = store.create(
            AutofillInteractionSource.LEGACY,
            scope,
            AutofillInteractionState.Authorized,
        )
        store.recordCandidates(interaction.id, emptySet(), savePromptsEnabled = true)

        val found = store.findSaveable(scope, AutofillInteractionSource.LEGACY)

        assertEquals(interaction.id, found?.id)
        assertTrue(store.beginSave(interaction.id) is AutofillInteractionUpdate.Updated)
        assertNull(store.findSaveable(scope, AutofillInteractionSource.LEGACY))
    }

    @Test
    fun `save does not associate with disabled mismatched or expired interactions`() = runBlocking {
        val store = AutofillInteractionStore()
        val scope = AutofillGrantContext("com.example.app", "example.com")
        val disabled = store.create(
            AutofillInteractionSource.LEGACY,
            scope,
            AutofillInteractionState.Authorized,
        )
        store.recordCandidates(disabled.id, emptySet(), savePromptsEnabled = false)
        val expired = store.create(
            AutofillInteractionSource.LEGACY,
            scope,
            AutofillInteractionState.Authorized,
        )
        store.recordCandidates(expired.id, emptySet(), savePromptsEnabled = true)
        store.finish(expired.id, AutofillInteractionState.Expired)

        assertNull(store.findSaveable(scope, AutofillInteractionSource.LEGACY))
        assertNull(
            store.findSaveable(
                AutofillGrantContext("com.other.app", "example.com"),
                AutofillInteractionSource.LEGACY,
            ),
        )
    }

    @Test
    fun `active legacy interactions can be enumerated for disconnect cleanup`() = runBlocking {
        val store = AutofillInteractionStore()
        val legacy = store.create(
            AutofillInteractionSource.LEGACY,
            AutofillGrantContext("com.example.legacy"),
            AutofillInteractionState.Authorized,
        )
        store.create(
            AutofillInteractionSource.CREDENTIAL_MANAGER,
            AutofillGrantContext("com.example.cm"),
            AutofillInteractionState.Authorized,
        )

        assertEquals(listOf(legacy.id), store.activeIds(AutofillInteractionSource.LEGACY))
    }
}

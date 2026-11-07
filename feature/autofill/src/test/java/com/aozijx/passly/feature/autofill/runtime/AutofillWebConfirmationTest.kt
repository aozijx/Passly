package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillWebConfirmationTest {
    @Test
    fun recipientAndSelectedEntryConfirmationAreBoundToOneLiveInteraction() =
        runBlocking {
            val store = AutofillInteractionStore()
            val recipient = AutofillGrantContext("com.example.browser", "example.com")
            val entryId = EntryId("entry-1")
            val interaction = store.create(AutofillInteractionSource.LEGACY, recipient, AutofillInteractionState.Authorized)

            assertFalse(store.isConfirmedWebSelectionReady(interaction.id, recipient, entryId, 1L))
            assertTrue(store.confirmWebRecipient(interaction.id, recipient, 1L) is AutofillInteractionUpdate.Updated)
            assertTrue(store.recordCandidates(interaction.id, setOf(entryId), sessionAtMs = 1L) is AutofillInteractionUpdate.Updated)
            assertTrue(store.confirmWebEntry(interaction.id, recipient, entryId, 1L) is AutofillInteractionUpdate.Updated)
            assertTrue(store.beginSelection(interaction.id, entryId, 1L) is AutofillInteractionUpdate.Updated)
            assertTrue(store.isConfirmedWebSelectionReady(interaction.id, recipient, entryId, 1L))
            assertTrue(store.claimWebResultDelivery(interaction.id, recipient, entryId, 1L))
            assertFalse(store.claimWebResultDelivery(interaction.id, recipient, entryId, 1L))
            assertFalse(store.finish(interaction.id, AutofillInteractionState.Cancelled).transitioned)
            assertEquals(null, store.expireIfIdle(interaction.id))
            store.releaseWebResultDeliveryClaim(interaction.id, recipient, entryId, 1L)

            store.finish(interaction.id, AutofillInteractionState.Cancelled)

            assertFalse(store.isConfirmedWebSelectionReady(interaction.id, recipient, entryId, 1L))
        }

    @Test
    fun recipientConfirmationRejectsChangedScopeAndNonLegacyInteraction() =
        runBlocking {
            val store = AutofillInteractionStore()
            val recipient = AutofillGrantContext("com.example.browser", "example.com")
            val interaction = store.create(AutofillInteractionSource.LEGACY, recipient, AutofillInteractionState.Authorized)
            val changedRecipient = AutofillGrantContext("com.example.browser", "attacker.example")

            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.SCOPE_MISMATCH),
                store.confirmWebRecipient(interaction.id, changedRecipient, 1L),
            )

            val nativeInteraction =
                store.create(
                    AutofillInteractionSource.LEGACY,
                    AutofillGrantContext("com.example.browser"),
                    AutofillInteractionState.Authorized,
                )
            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.INVALID_STATE),
                store.confirmWebRecipient(nativeInteraction.id, AutofillGrantContext("com.example.browser"), 1L),
            )
        }

    @Test
    fun entryConfirmationRequiresRecipientApprovalAndPresentedCandidate() =
        runBlocking {
            val store = AutofillInteractionStore()
            val recipient = AutofillGrantContext("com.example.browser", "example.com")
            val entryId = EntryId("entry-1")
            val interaction = store.create(AutofillInteractionSource.LEGACY, recipient, AutofillInteractionState.Authorized)

            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.WEB_ORIGIN_NOT_CONFIRMED),
                store.confirmWebEntry(interaction.id, recipient, entryId, 1L),
            )
            store.confirmWebRecipient(interaction.id, recipient, 1L)
            store.recordCandidates(interaction.id, emptySet(), sessionAtMs = 1L)

            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED),
                store.confirmWebEntry(interaction.id, recipient, entryId, 1L),
            )
        }
}

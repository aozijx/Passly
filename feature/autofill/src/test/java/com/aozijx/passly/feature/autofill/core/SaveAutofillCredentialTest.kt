package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.autofill.port.AutofillEntryCreation
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveAutofillCredentialTest {
    @Test
    fun invalidDraftsAreRejectedBeforePersistence() =
        runBlocking {
            val cases =
                listOf(
                    "blank password" to draft(password = "  "),
                    "invalid website domain" to draft(password = "secret", webDomain = "not a valid domain"),
                )

            cases.forEach { (name, candidate) ->
                val fixture = fixture()

                assertEquals(name, AutofillSaveOutcome.InvalidDraft, fixture.operation(candidate))
                assertEquals(name, 0, fixture.entryCreation.createCalls)
            }
        }

    @Test
    fun passwordOnlyDraftCreatesTypedLoginDraft() =
        runBlocking {
            val fixture = fixture()

            val outcome = fixture.operation(draft(username = "", password = "secret"))

            assertEquals(AutofillSaveOutcome.Saved(EntryId("saved-entry")), outcome)
            val savedDraft = requireNotNull(fixture.entryCreation.savedDraft)
            assertEquals(EntryType.LOGIN, savedDraft.type)
            assertEquals(null, savedDraft[FieldKey.USERNAME])
            assertEquals(EntryDraftValue.Text("secret"), savedDraft[FieldKey.PASSWORD])
            assertTrue(
                savedDraft.values.keys.all { it in setOf(FieldKey.TITLE, FieldKey.PASSWORD, FieldKey.PRIMARY_URL, FieldKey.DOMAINS) },
            )
        }

    @Test
    fun inboxDraftIdentityIsForwardedToApplicationCreationPort() =
        runBlocking {
            val fixture = fixture()
            val inboxDraftId = AutofillInboxDraftId("00112233445566778899aabbccddeeff")

            fixture.operation(draft(password = "secret"), inboxDraftId)
            fixture.operation(draft(password = "secret"), inboxDraftId)

            assertEquals(listOf(inboxDraftId, inboxDraftId), fixture.entryCreation.inboxDraftIds)
        }

    private fun fixture(): Fixture {
        val entryCreation = RecordingEntryCreation()
        return Fixture(
            operation =
                SaveAutofillCredential(
                    applicationLabelResolver = ApplicationLabelResolver { "Example" },
                    createEntry = entryCreation,
                ),
            entryCreation = entryCreation,
        )
    }

    private fun draft(
        username: String = "alice",
        password: String,
        webDomain: String? = "example.com",
    ) = AutofillSaveDraft(
        packageName = "com.example.app",
        webDomain = webDomain,
        pageTitle = "Example",
        username = username,
        password = password,
    )

    private data class Fixture(
        val operation: SaveAutofillCredential,
        val entryCreation: RecordingEntryCreation,
    )

    private class RecordingEntryCreation : AutofillEntryCreation {
        var createCalls = 0
        var savedDraft: EntryDraft? = null
        val inboxDraftIds = mutableListOf<AutofillInboxDraftId?>()

        override suspend fun createFromAutofill(
            draft: EntryDraft,
            inboxDraftId: AutofillInboxDraftId?,
        ): AppResult<EntryId> {
            inboxDraftIds += inboxDraftId
            return persist(draft)
        }

        private fun persist(draft: EntryDraft): AppResult<EntryId> {
            createCalls++
            savedDraft = draft
            return AppResult.Success(EntryId("saved-entry"))
        }
    }
}

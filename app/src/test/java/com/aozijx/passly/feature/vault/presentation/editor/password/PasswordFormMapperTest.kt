package com.aozijx.passly.feature.vault.presentation.editor.password

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordFormMapperTest {
    @Test
    fun fromEntryRestoresFieldsAndRetainsAdditionalAssociatedAppsInTheUpdateDraft() {
        val entry = Entry(
            EntryIdentity(EntryId("login"), EntryType.LOGIN, timestamps = EntryTimestamps(1)),
            EntryProfile(
                "Mail",
                username = "ada",
                associations = com.aozijx.passly.domain.entry.model.EntryAssociations(
                    primaryUrl = "https://mail.example",
                    applicationIds = linkedSetOf("mail.app", "browser.app"),
                ),
                tags = setOf("work"),
            ),
            EntrySecret(credential = LoginCredential(password = "secret"), notes = "note"),
        )

        val draft = AddPasswordFormState().fromEntry(entry).toEntryDraft()

        assertEquals(EntryDraftValue.Text("secret"), draft[FieldKey.PASSWORD])
        assertEquals(EntryDraftValue.Text("ada"), draft[FieldKey.USERNAME])
        assertEquals(EntryDraftValue.TextList(listOf("mail.app", "browser.app")), draft[FieldKey.APPLICATION_IDS])
        assertEquals(EntryDraftValue.Text("note"), draft[FieldKey.NOTES])
    }

    @Test
    fun toEntryDraft_normalizesMetadataAndPreservesPassword() {
        val draft = AddPasswordFormState(
            title = "  Mail  ", username = " user@example.com ", password = " secret ",
            websiteUrl = " https://example.com ", notes = " personal ", tags = " work, , mail, work ",
            associatedAppPackage = "com.example.mail",
        ).toEntryDraft()

        assertEquals(EntryType.LOGIN, draft.type)
        assertEquals(EntryDraftValue.Text("Mail"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("user@example.com"), draft[FieldKey.USERNAME])
        assertEquals(EntryDraftValue.Text(" secret "), draft[FieldKey.PASSWORD])
        assertEquals(EntryDraftValue.Text("https://example.com"), draft[FieldKey.PRIMARY_URL])
        assertEquals(EntryDraftValue.Text("personal"), draft[FieldKey.NOTES])
        assertEquals(
            EntryDraftValue.TextList(listOf("com.example.mail")),
            draft[FieldKey.APPLICATION_IDS],
        )
        assertEquals(EntryDraftValue.TextList(listOf("work", "mail", "work")), draft[FieldKey.TAGS])
        assertTrue(draft.missingRequiredFields(EntryTypeDefinitions[EntryType.LOGIN]).isEmpty())
    }

    @Test
    fun toEntryDraft_omitsBlankOptionalMetadata() {
        val draft = AddPasswordFormState(
            title = "Account",
            password = "secret",
            username = " ",
            websiteUrl = " ",
            notes = " ",
            tags = " , ",
            associatedAppPackage = null,
        ).toEntryDraft()

        assertEquals(null, draft[FieldKey.USERNAME])
        assertEquals(null, draft[FieldKey.PRIMARY_URL])
        assertEquals(null, draft[FieldKey.NOTES])
        assertEquals(null, draft[FieldKey.TAGS])
        assertEquals(null, draft[FieldKey.APPLICATION_IDS])
    }
}

package com.aozijx.passly.presentation.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordFormMapperTest {
    @Test
    fun toEntryDraft_normalizesMetadataAndPreservesPassword() {
        val draft = AddPasswordFormState(
            title = "  Mail  ", username = " user@example.com ", password = " secret ",
            website = " https://example.com ", notes = " personal ", tags = " work, , mail, work ",
        ).toEntryDraft()

        assertEquals(EntryDraftTarget.New(EntryType.LOGIN), draft.target)
        assertEquals(EntryDraftValue.Text("Mail"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("user@example.com"), draft[FieldKey.USERNAME])
        assertEquals(EntryDraftValue.Text(" secret "), draft[FieldKey.PASSWORD])
        assertEquals(EntryDraftValue.Text("https://example.com"), draft[FieldKey.PRIMARY_URL])
        assertEquals(EntryDraftValue.Text("personal"), draft[FieldKey.NOTES])
        assertEquals(EntryDraftValue.TextList(listOf("work", "mail", "work")), draft[FieldKey.TAGS])
        assertTrue(draft.missingRequiredFields(EntryTypeDefinitions[EntryType.LOGIN]).isEmpty())
    }

    @Test
    fun toEntryDraft_omitsBlankOptionalMetadata() {
        val draft = AddPasswordFormState(
            title = "Account",
            password = "secret",
            username = " ",
            website = " ",
            notes = " ",
            tags = " , ",
        ).toEntryDraft()

        assertEquals(null, draft[FieldKey.USERNAME])
        assertEquals(null, draft[FieldKey.PRIMARY_URL])
        assertEquals(null, draft[FieldKey.NOTES])
        assertEquals(null, draft[FieldKey.TAGS])
    }
}

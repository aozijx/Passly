package com.aozijx.passly.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordEntryFactoryTest {

    @Test
    fun createMapsEveryPersistedFieldAndPreservesPasswordWhitespace() {
        val entry = PasswordEntryFactory.create(
            state = AddPasswordFormState(
                title = "  Mail  ",
                username = "  user@example.com  ",
                password = " secret ",
                website = "  https://example.com  ",
                notes = "  personal account  "
            ),
            now = 123L
        )

        assertEquals(EntryType.LOGIN, entry.type)
        assertEquals("Mail", entry.profile.title)
        assertEquals("user@example.com", entry.profile.username)
        assertEquals("https://example.com", entry.profile.associations.primaryUrl)
        assertEquals(" secret ", entry.secret.login?.password)
        assertEquals("personal account", entry.secret.notes)
        assertEquals(123L, entry.timestamps.createdAtMs)
        assertEquals(123L, entry.timestamps.updatedAtMs)
    }

    @Test
    fun createOmitsUndefinedOptionalFields() {
        val entry = PasswordEntryFactory.create(
            state = AddPasswordFormState(
                title = "Account",
                password = "secret",
                website = "   ",
                notes = "   "
            )
        )

        assertNull(entry.profile.associations.primaryUrl)
        assertNull(entry.secret.notes)
    }
}

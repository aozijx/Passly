package com.aozijx.passly.data.mapper.search

import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.query.LookupField
import org.junit.Assert.assertEquals
import org.junit.Test

class EntrySearchFieldMapperTest {

    @Test
    fun `all autofill associations are added to the blind index`() {
        val entry = Entry(
            header = EntryIdentity(
                id = EntryId("entry"),
                entryType = EntryType.LOGIN,
                version = EntryVersion.INITIAL,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            summary = EntryProfile(
                title = "Example",
                username = "person",
                website = EntryAssociations(
                    primaryUrl = "https://login.example.com/account",
                    matchDomains = setOf("example.com", "login.example.com"),
                    packageNames = setOf("com.example.one", "com.example.two"),
                ),
            ),
            secret = EntrySecret(),
        )

        val fields = entry.toLookupFields()

        assertEquals(
            setOf("example.com", "login.example.com"),
            fields.filter { it.field == LookupField.DOMAIN }.map { it.text }.toSet(),
        )
        assertEquals(
            setOf("com.example.one", "com.example.two"),
            fields.filter { it.field == LookupField.PACKAGE }.map { it.text }.toSet(),
        )
        assertEquals(
            listOf("https://login.example.com/account"),
            fields.filter { it.field == LookupField.URL }.map { it.text },
        )
    }
}

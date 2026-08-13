package com.aozijx.passly.data.mapper.search

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.LookupField
import org.junit.Assert.assertEquals
import org.junit.Test

class EntrySearchFieldMapperTest {

    @Test
    fun `all autofill associations are added to the blind index`() {
        val entry = EntryAggregate(
            header = EntryHeader(
                id = EntryId("entry"),
                entryType = EntryType.LOGIN,
                version = EntryVersion.INITIAL,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            summary = EntrySummary(
                title = "Example",
                username = "person",
                website = WebsiteInfo(
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

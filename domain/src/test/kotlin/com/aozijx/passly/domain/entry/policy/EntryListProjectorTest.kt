package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryListProjectorTest {

    @Test
    fun `search category and capability use one query definition`() {
        val mail = item(
            id = "mail",
            title = "Mail",
            tags = setOf("Work"),
            domains = setOf("example.com"),
            capabilities = EntryCapabilities(setOf(EntryCapability.PASSWORD)),
        )
        val otp = item(
            id = "otp",
            title = "Authenticator",
            tags = setOf("Work"),
            capabilities = EntryCapabilities(setOf(EntryCapability.OTP)),
        )

        val result = EntryListProjector.project(
            listOf(mail, otp),
            EntryListQuery(
                searchText = "example",
                filter = EntryFilter.PASSWORD_ONLY,
                category = "work",
            ),
        )

        assertEquals(listOf(mail), result)
    }

    @Test
    fun `categories are normalized and case insensitive`() {
        val categories = EntryListProjector.categories(
            listOf(
                item("1", "One", tags = setOf(" Work ", "Personal")),
                item("2", "Two", tags = setOf("work")),
            ),
        )

        assertEquals(listOf("Personal", "Work"), categories)
    }

    private fun item(
        id: String,
        title: String,
        tags: Set<String> = emptySet(),
        domains: Set<String> = emptySet(),
        capabilities: EntryCapabilities = EntryCapabilities(),
    ) = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(id),
            type = EntryType.LOGIN,
            timestamps = EntryTimestamps(0L),
        ),
        profile = EntryProfile(
            title = title,
            tags = tags,
            associations = EntryAssociations(domains = domains),
        ),
        capabilities = capabilities,
    )
}

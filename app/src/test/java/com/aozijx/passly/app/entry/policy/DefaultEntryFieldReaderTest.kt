package com.aozijx.passly.app.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultEntryFieldReaderTest {
    private val reader = DefaultEntryFieldReader()
    private val entry = Entry(
        identity = EntryIdentity(
            id = EntryId("entry-id"),
            type = EntryType.ACCOUNT,
            timestamps = EntryTimestamps(createdAtMs = 1L),
        ),
        profile = EntryProfile(
            title = "Example",
            username = "display-name",
            associations = EntryAssociations(
                primaryUrl = "https://example.com/login",
                domains = linkedSetOf("example.com", "example.org"),
            ),
        ),
    )

    @Test
    fun readsProfileBackedSemanticFieldKeys() {
        assertEquals("display-name", reader.getFieldValue(entry, FieldKey.CARD_HOLDER))
        assertEquals("display-name", reader.getFieldValue(entry, FieldKey.WIFI_SSID))
    }
    @Test
    fun readsPrimaryUrlFromItsDedicatedFieldKey() {
        assertEquals(
            "https://example.com/login",
            reader.getFieldValue(entry, FieldKey.PRIMARY_URL),
        )
    }

    @Test
    fun readsDomainsFromTheirDedicatedFieldKey() {
        assertEquals(
            "example.com, example.org",
            reader.getFieldValue(entry, FieldKey.DOMAINS),
        )
    }
}

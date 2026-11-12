package com.aozijx.passly.domain.entry.model.relation

import com.aozijx.passly.domain.entry.model.EntryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryLinkTest {
    @Test
    fun `generic link canonicalizes endpoint order`() {
        val link =
            EntryLink.create(
                id = EntryLinkId("link"),
                firstEntryId = EntryId("z"),
                secondEntryId = EntryId("a"),
                createdAt = 1L,
            )

        assertEquals(EntryId("a"), link.firstEntryId)
        assertEquals(EntryId("z"), link.secondEntryId)
    }

    @Test
    fun `self link is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntryLink.create(
                id = EntryLinkId("link"),
                firstEntryId = EntryId("same"),
                secondEntryId = EntryId("same"),
                createdAt = 1L,
            )
        }
    }
}

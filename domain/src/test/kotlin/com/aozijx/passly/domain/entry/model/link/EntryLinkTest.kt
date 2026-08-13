package com.aozijx.passly.domain.entry.model.link

import com.aozijx.passly.domain.entry.model.EntryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryLinkTest {
    @Test
    fun `related link canonicalizes endpoints`() {
        val link = EntryLink.create(
            id = EntryLinkId("link"),
            sourceEntryId = EntryId("z"),
            targetEntryId = EntryId("a"),
            relationType = EntryRelationType.RELATED_TO,
            createdAt = 1L
        )

        assertEquals(EntryId("a"), link.sourceEntryId)
        assertEquals(EntryId("z"), link.targetEntryId)
    }

    @Test
    fun `directional link preserves direction`() {
        val link = EntryLink.create(
            id = EntryLinkId("link"),
            sourceEntryId = EntryId("login"),
            targetEntryId = EntryId("account"),
            relationType = EntryRelationType.MEMBER_OF_ACCOUNT,
            createdAt = 1L
        )

        assertEquals(EntryId("login"), link.sourceEntryId)
        assertEquals(EntryId("account"), link.targetEntryId)
    }

    @Test
    fun `self link is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntryLink.create(
                id = EntryLinkId("link"),
                sourceEntryId = EntryId("same"),
                targetEntryId = EntryId("same"),
                relationType = EntryRelationType.OTP_FOR,
                createdAt = 1L
            )
        }
    }
}

package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRelatedEntryIdsTest {

    @Test
    fun `account resolves all members without including itself`() {
        assertEquals(
            setOf(EntryId("login"), EntryId("otp")),
            DetailRelatedEntryIds.resolve(
                entryId = EntryId("account"),
                entryType = EntryType.ACCOUNT,
                links = links(),
            ),
        )
    }

    @Test
    fun `member resolves its account and sibling entries`() {
        assertEquals(
            setOf(EntryId("account"), EntryId("otp")),
            DetailRelatedEntryIds.resolve(
                entryId = EntryId("login"),
                entryType = EntryType.LOGIN,
                links = links(),
            ),
        )
    }

    @Test
    fun `unlinked entry has no related entries`() {
        assertEquals(
            emptySet<EntryId>(),
            DetailRelatedEntryIds.resolve(EntryId("note"), EntryType.NOTE, links()),
        )
    }

    private fun links() = listOf(
        link("member", "login", "account", EntryRelationType.MEMBER_OF_ACCOUNT),
        link("otp", "otp", "login", EntryRelationType.OTP_FOR),
    )

    private fun link(
        id: String,
        source: String,
        target: String,
        type: EntryRelationType,
    ) = EntryLink.create(
        id = EntryLinkId(id),
        sourceEntryId = EntryId(source),
        targetEntryId = EntryId(target),
        relationType = type,
        createdAt = 1L,
    )
}

package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryAccountGraphTest {
    @Test
    fun `otp resolves account through login`() {
        val graph = EntryAccountGraph(
            listOf(
                link("member", "login", "account", EntryRelationType.MEMBER_OF_ACCOUNT),
                link("otp", "otp", "login", EntryRelationType.OTP_FOR)
            )
        )

        assertEquals(EntryId("account"), graph.accountFor(EntryId("otp")))
        assertEquals(
            setOf(EntryId("login"), EntryId("otp")),
            graph.membersOf(EntryId("account"))
        )
    }

    private fun link(
        id: String,
        source: String,
        target: String,
        type: EntryRelationType
    ) = EntryLink.create(
        id = EntryLinkId(id),
        sourceEntryId = EntryId(source),
        targetEntryId = EntryId(target),
        relationType = type,
        createdAt = 1L
    )
}

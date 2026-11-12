package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryAccountGraphTest {
    @Test
    fun `account graph infers membership, recovery, and otp from endpoint types`() {
        val graph =
            EntryAccountGraph(
                listOf(
                    link("member", "login", "account"),
                    link("recovery", "recovery", "account"),
                    link("otp", "otp", "login"),
                ),
                mapOf(
                    EntryId("login") to EntryType.LOGIN,
                    EntryId("account") to EntryType.ACCOUNT,
                    EntryId("recovery") to EntryType.RECOVERY_CODE,
                    EntryId("otp") to EntryType.OTP,
                ),
            )

        assertEquals(EntryId("account"), graph.accountFor(EntryId("login")))
        assertEquals(EntryId("account"), graph.accountFor(EntryId("recovery")))
        assertEquals(EntryId("account"), graph.accountFor(EntryId("otp")))
        assertEquals(
            setOf(EntryId("login"), EntryId("recovery"), EntryId("otp")),
            graph.membersOf(EntryId("account")),
        )
    }

    private fun link(
        id: String,
        first: String,
        second: String,
    ) = EntryLink.create(
        id = EntryLinkId(id),
        firstEntryId = EntryId(first),
        secondEntryId = EntryId(second),
        createdAt = 1L,
    )
}

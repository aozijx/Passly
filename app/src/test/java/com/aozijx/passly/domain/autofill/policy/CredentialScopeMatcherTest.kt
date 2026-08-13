package com.aozijx.passly.domain.autofill.policy

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialScopeMatcherTest {

    private val entry = EntryAggregate(
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
            website = WebsiteInfo(matchDomains = setOf("login.example.com")),
        ),
        secret = EntrySecret(),
    )

    @Test
    fun `URL forms normalize to the same exact host`() {
        assertTrue(
            CredentialScopeMatcher.matches(
                entry,
                packageName = null,
                webDomain = "https://LOGIN.example.com/account",
            )
        )
    }

    @Test
    fun `lookalike suffix is not accepted`() {
        assertFalse(
            CredentialScopeMatcher.matches(
                entry,
                packageName = null,
                webDomain = "login.example.com.attacker.test",
            )
        )
    }
}

package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.MatchType
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillCandidateMappingTest {
    @Test
    fun `mapping filters unrelated candidates from many rows`() {
        val unrelated =
            List(50) { index ->
                EntryEntity(
                    entryId = "near-$index",
                    entryType = EntryType.LOGIN,
                    title = "Near match",
                    domains = setOf("evil-example.com"),
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            }
        val lateRelated =
            EntryEntity(
                entryId = "related-late",
                entryType = EntryType.LOGIN,
                title = "Related",
                domains = setOf("login.example.com"),
                createdAt = 1L,
                updatedAt = 1L,
            )

        val matches = (unrelated + lateRelated).toMatchingAutofillCandidates(null, "example.com", strictDomainMatching = false)

        assertEquals(listOf("related-late"), matches.map { it.entry.id.value })
    }

    @Test
    fun `web candidate uses same registrable domain only when relaxed matching is enabled`() {
        val entity =
            EntryEntity(
                entryId = "subdomain",
                entryType = EntryType.LOGIN,
                title = "Subdomain",
                domains = setOf("https://login.example.co.uk/path"),
                createdAt = 1L,
                updatedAt = 1L,
            )

        assertEquals(MatchType.UNKNOWN, entity.autofillMatch(null, "example.co.uk").type)
        assertEquals(
            MatchType.WEB_DOMAIN,
            entity.autofillMatch(null, "example.co.uk", strictDomainMatching = false).type,
        )
        assertEquals(
            MatchType.UNKNOWN,
            entity
                .copy(domains = setOf("a.blogspot.com"))
                .autofillMatch(null, "b.blogspot.com", strictDomainMatching = false)
                .type,
        )
        assertFalse(AutofillScope.matchesDomain("evil-example.com", "example.com", strict = false))
    }

    @Test
    fun mapsOnlyCandidateMetadataAndCapabilities() {
        val candidate =
            EntryEntity(
                entryId = "entry-1",
                entryType = EntryType.LOGIN,
                title = "Example",
                username = "person@example.com",
                primaryUrl = "https://example.com/login",
                domains = setOf("example.com"),
                applicationIds = setOf("com.example.app"),
                favorite = true,
                capabilityFlags = 1,
                createdAt = 10L,
                updatedAt = 20L,
            ).toAutofillCandidateSummary(
                CredentialMatch(
                    type = MatchType.APPLICATION_ID,
                    applicationId = "com.example.app",
                ),
            )

        assertEquals("entry-1", candidate.entry.id.value)
        assertEquals("Example", candidate.entry.title)
        assertEquals("person@example.com", candidate.entry.username)
        assertEquals(MatchType.APPLICATION_ID, candidate.match.type)
        assertTrue(EntryCapability.PASSWORD in candidate.entry.capabilities)
        assertNull(candidate.entry.otpPreview)
    }

    @Test
    fun candidateContractHasNoSecretOrPlaintextPasswordProperty() {
        val fieldNames = metadataFieldNames(AutofillCandidateSummary::class.java)

        assertFalse(fieldNames.any { it.contains("secret", ignoreCase = true) })
        assertFalse(fieldNames.any { it.contains("password", ignoreCase = true) })
        assertFalse(fieldNames.any { it.contains("otpCode", ignoreCase = true) })
    }

    private fun metadataFieldNames(
        type: Class<*>,
        visited: MutableSet<Class<*>> = mutableSetOf(),
    ): Set<String> {
        if (!type.name.startsWith("com.aozijx.passly.domain.") || !visited.add(type)) return emptySet()

        return type.declaredFields
            .asSequence()
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .flatMap { field ->
                sequenceOf(field.name) + metadataFieldNames(field.type, visited).asSequence()
            }.toSet()
    }

    @Test
    fun candidateDomainMatchUsesExactCanonicalHost() {
        val nearDomain = entry("evil-example.com").autofillMatch(applicationId = null, domain = "example.com")
        val exactDomain = entry("WWW.Example.com.").autofillMatch(applicationId = null, domain = "www.example.com")

        assertEquals(MatchType.UNKNOWN, nearDomain.type)
        assertEquals(MatchType.WEB_DOMAIN, exactDomain.type)
    }

    private fun entry(domain: String) =
        EntryEntity(
            entryId = "entry-$domain",
            entryType = EntryType.LOGIN,
            title = domain,
            domains = setOf(domain),
            createdAt = 1L,
            updatedAt = 1L,
        )
}

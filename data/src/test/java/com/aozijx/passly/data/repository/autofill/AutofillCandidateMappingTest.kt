package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillCandidateMappingTest {
    @Test
    fun mapsOnlyCandidateMetadataAndCapabilities() {
        val candidate = EntryEntity(
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
    }

    @Test
    fun candidateContractHasNoSecretOrPlaintextPasswordProperty() {
        val fieldNames = AutofillCandidateSummary::class.java.declaredFields
            .mapTo(mutableSetOf()) { it.name }

        assertFalse("secret" in fieldNames)
        assertFalse("password" in fieldNames)
        assertFalse("otpCode" in fieldNames)
    }
}

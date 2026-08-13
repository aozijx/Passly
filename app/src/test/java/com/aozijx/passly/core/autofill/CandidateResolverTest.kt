package com.aozijx.passly.core.autofill

import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.data.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.data.settings.model.AutofillSettings
import com.aozijx.passly.data.settings.model.AutofillPresentation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandidateResolverTest {

    @Test
    fun `UUID candidate identity is preserved through the pipeline`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.app"),
        )
        val repository = FakeCredentialRepository(entry)
        val resolver = CandidateResolver(repository)

        val result = resolver.resolve(
            InternalFillRequest(
                parentPackage = "com.example.app",
                fields = listOf(
                    FieldDescriptor("password", autofillHints = listOf("PASSWORD"))
                ),
            ),
            AutofillSettings(maxSuggestions = 7),
        )

        assertEquals(entry.id.value, result.single().candidateId)
        assertEquals(7, repository.lastLimit)
        assertEquals(false, repository.lastIncludeSecrets)
    }

    @Test
    fun `inline fill without authentication does not preload secrets`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.app"),
        )
        val repository = FakeCredentialRepository(entry)

        CandidateResolver(repository).resolve(
            InternalFillRequest(
                parentPackage = "com.example.app",
                fields = emptyList(),
            ),
            AutofillSettings(
                requireAuthentication = false,
                presentation = AutofillPresentation.SYSTEM_INLINE,
            ),
        )

        assertEquals(false, repository.lastIncludeSecrets)
    }

    @Test
    fun `bottom sheet candidates never preload secrets`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.app"),
        )
        val repository = FakeCredentialRepository(entry)

        CandidateResolver(repository).resolve(
            InternalFillRequest(
                parentPackage = "com.example.app",
                fields = emptyList(),
            ),
            AutofillSettings(
                requireAuthentication = false,
                presentation = AutofillPresentation.BOTTOM_SHEET,
            ),
        )

        assertEquals(false, repository.lastIncludeSecrets)
    }

    @Test
    fun `explicit second phase lookup can load secrets`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.app"),
        )
        val repository = FakeCredentialRepository(entry)

        CandidateResolver(repository).resolveByPackage(
            packageName = "com.example.app",
            webDomain = null,
            settings = AutofillSettings(requireAuthentication = false),
            includeSecrets = true,
        )

        assertEquals(true, repository.lastIncludeSecrets)
    }

    @Test
    fun `selected credential must belong to the requesting app`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.owner"),
        )
        val resolver = CandidateResolver(FakeCredentialRepository(entry))

        val result = resolver.resolveSelected(
            entryId = entry.id.value,
            packageName = "com.example.attacker",
            webDomain = null,
            settings = AutofillSettings(allowUnmatchedSuggestions = false),
        )

        assertNull(result)
    }

    private class FakeCredentialRepository(
        private val entry: Entry,
    ) : CredentialServiceRepository {
        var lastLimit: Int = 0
        var lastIncludeSecrets: Boolean? = null

        override suspend fun search(
            packageName: String?,
            webDomain: String?,
            allowUnmatched: Boolean,
            includeSecrets: Boolean,
            limit: Int,
        ): List<CredentialCandidate> {
            lastLimit = limit
            lastIncludeSecrets = includeSecrets
            return listOf(
                CredentialCandidate(
                    entry = entry,
                    match = CredentialMatch(
                        type = MatchType.APPLICATION_ID,
                        applicationId = packageName,
                    ),
                )
            )
        }

        override suspend fun getById(entryId: String): Entry? =
            entry.takeIf { it.id.value == entryId }

        override suspend fun getByIds(
            entryIds: List<String>,
            includeSecrets: Boolean
        ): List<Entry> =
            listOf(entry).filter { it.id.value in entryIds }

        override suspend fun save(
            packageName: String?,
            webDomain: String?,
            pageTitle: String?,
            usernameValue: String,
            passwordValue: String,
        ): Boolean = true
    }

    private fun loginEntry(
        id: String,
        packages: Set<String>,
    ): Entry = Entry(
        identity = EntryIdentity(
            id = EntryId(id),
            type = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(createdAtMs = 1L, updatedAtMs = 2L),
        ),
        profile = EntryProfile(
            title = "Example",
            username = "person@example.com",
            associations = EntryAssociations(applicationIds = packages),
        ),
        secret = EntrySecret(credential = LoginCredential(password = "secret")),
    )
}

package com.aozijx.passly.core.autofill

import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.entry.model.lookup.MatchType
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.AutofillPresentation
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

        assertEquals(entry.id, result.single().candidateId)
        assertEquals(7, repository.lastLimit)
        assertEquals(false, repository.lastIncludeSecrets)
    }

    @Test
    fun `inline fill without authentication loads fill values`() = runBlocking {
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

        assertEquals(true, repository.lastIncludeSecrets)
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
    fun `selected credential must belong to the requesting app`() = runBlocking {
        val entry = loginEntry(
            id = "018f9dd6-66c5-7cc0-85b5-39a337956681",
            packages = setOf("com.example.owner"),
        )
        val resolver = CandidateResolver(FakeCredentialRepository(entry))

        val result = resolver.resolveSelected(
            entryId = entry.id,
            packageName = "com.example.attacker",
            webDomain = null,
            settings = AutofillSettings(allowUnmatchedSuggestions = false),
        )

        assertNull(result)
    }

    private class FakeCredentialRepository(
        private val entry: VaultEntry,
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
                    score = MatchType.PACKAGE_NAME.score,
                    matchedBy = MatchType.PACKAGE_NAME,
                    matchedPackage = packageName,
                )
            )
        }

        override suspend fun getById(entryId: String): VaultEntry? =
            entry.takeIf { it.id == entryId }

        override suspend fun getByIds(
            entryIds: List<String>,
            includeSecrets: Boolean
        ): List<VaultEntry> =
            listOf(entry).filter { it.id in entryIds }

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
    ): VaultEntry = VaultEntry(
        header = EntryHeader(
            id = EntryId(id),
            entryType = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            createdAt = 1L,
            updatedAt = 2L,
        ),
        summary = EntrySummary(
            title = "Example",
            username = "person@example.com",
            website = WebsiteInfo(packageNames = packages),
        ),
        secret = EntrySecret(login = LoginSecret(password = "secret")),
    )
}

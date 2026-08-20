package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.settings.model.InteractionSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateRetrieverTest {

    private val repository = object : CredentialServiceRepository {
        override suspend fun search(
            packageName: String?,
            webDomain: String?,
            allowUnmatched: Boolean,
            includeSecrets: Boolean,
            limit: Int
        ): List<CredentialCandidate> = emptyList()

        override suspend fun getById(entryId: String): Entry? = null
        override suspend fun getByIds(entryIds: List<String>, includeSecrets: Boolean): List<Entry> = emptyList()
        override suspend fun save(
            packageName: String?,
            webDomain: String?,
            pageTitle: String?,
            usernameValue: String,
            passwordValue: String
        ): Boolean = false
    }

    private val retriever = CandidateRetriever(repository)
    private val settings = InteractionSettings().autofill

    @Test
    fun `resolve delegates to search`() = runBlocking {
        val request = AutofillRequest(
            packageName = "com.example",
            domain = null,
            fields = listOf(
                AutofillField(
                    id = "id/1",
                    hints = emptySet(),
                    inputType = null,
                    isFocused = false,
                )
            ),
            source = AutofillSource.AUTOFILL_SERVICE,
        )
        val results = retriever.resolve(request, settings)
        assertEquals(0, results.size)
    }

    @Test(expected = IllegalStateException::class)
    fun `resolve propagates repository errors`() = runBlocking {
        CandidateRetriever(ThrowingCredentialRepository).resolve(
            AutofillRequest(
                packageName = "com.example",
                domain = null,
                fields = emptyList(),
                source = AutofillSource.AUTOFILL_SERVICE,
            ),
            settings,
        )
        Unit
    }

    private object ThrowingCredentialRepository : CredentialServiceRepository {
        override suspend fun search(
            packageName: String?,
            webDomain: String?,
            allowUnmatched: Boolean,
            includeSecrets: Boolean,
            limit: Int,
        ): List<CredentialCandidate> = throw IllegalStateException("query failed")

        override suspend fun getById(entryId: String): Entry? = null
        override suspend fun getByIds(
            entryIds: List<String>,
            includeSecrets: Boolean,
        ): List<Entry> = emptyList()

        override suspend fun save(
            packageName: String?,
            webDomain: String?,
            pageTitle: String?,
            usernameValue: String,
            passwordValue: String,
        ): Boolean = false
    }
}

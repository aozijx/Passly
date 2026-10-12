package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillCredentialResolutionTest {
    @Test
    fun activeLoginWithPasswordResolvesTypedPayload() {
        val resolution = resolveAutofillCredential(
            entity = loginEntity(),
            password = "secret",
            otpCode = null,
        )

        val payload = (resolution as AutofillCredentialResolution.Resolved).payload
        assertEquals("entry-1", payload.entryId.value)
        assertEquals("person@example.com", payload.username)
        assertEquals("secret", payload.password)
        assertEquals(null, payload.otpCode)
    }

    @Test
    fun missingEntryIsNotFound() {
        assertSame(
            AutofillCredentialResolution.NotFound,
            resolveAutofillCredential(entity = null, password = "secret", otpCode = null),
        )
    }

    @Test
    fun deletedOrNonLoginEntryIsNotFillable() {
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity().copy(deletedAt = 30L),
                password = "secret",
                otpCode = null,
            ),
        )
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity().copy(entryType = EntryType.NOTE),
                password = "secret",
                otpCode = null,
            ),
        )
    }

    @Test
    fun blankPasswordIsNotFillable() {
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity(),
                password = "  ",
                otpCode = null,
            ),
        )
    }

    private fun loginEntity(): EntryEntity = EntryEntity(
        entryId = "entry-1",
        entryType = EntryType.LOGIN,
        title = "Example",
        username = "person@example.com",
        createdAt = 10L,
        updatedAt = 20L,
    )
}

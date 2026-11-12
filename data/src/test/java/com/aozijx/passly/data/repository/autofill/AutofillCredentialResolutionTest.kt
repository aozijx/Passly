package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.domain.autofill.model.AutofillCredentialField
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillCredentialResolutionTest {
    @Test
    fun resolvesOnlyRequestedFields() {
        val resolution =
            resolveAutofillCredential(
                entity = loginEntity(),
                requestedFields = setOf(AutofillCredentialField.USERNAME),
                values =
                    mapOf(
                        AutofillCredentialField.USERNAME to "person@example.com",
                        AutofillCredentialField.PASSWORD to "unrequested-secret",
                    ),
            )

        val payload = (resolution as AutofillCredentialResolution.Resolved).payload
        assertEquals("entry-1", payload.entryId.value)
        assertEquals(mapOf(AutofillCredentialField.USERNAME to "person@example.com"), payload.fields)
    }

    @Test
    fun missingEntryIsNotFound() {
        assertSame(
            AutofillCredentialResolution.NotFound,
            resolveAutofillCredential(entity = null, requestedFields = emptySet(), values = emptyMap()),
        )
    }

    @Test
    fun deletedOrNonLoginEntryIsNotFillable() {
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity().copy(deletedAt = 30L),
                requestedFields = setOf(AutofillCredentialField.PASSWORD),
                values = mapOf(AutofillCredentialField.PASSWORD to "secret"),
            ),
        )
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity().copy(entryType = EntryType.NOTE),
                requestedFields = setOf(AutofillCredentialField.PASSWORD),
                values = mapOf(AutofillCredentialField.PASSWORD to "secret"),
            ),
        )
    }

    @Test
    fun blankPasswordIsNotFillable() {
        assertSame(
            AutofillCredentialResolution.NotFillable,
            resolveAutofillCredential(
                entity = loginEntity(),
                requestedFields = setOf(AutofillCredentialField.PASSWORD),
                values = mapOf(AutofillCredentialField.PASSWORD to "  "),
            ),
        )
    }

    @Test
    fun linkedOtpIsSelectedOnlyWhenItTargetsTheLogin() {
        val links =
            listOf(
                link("unrelated", "otp-1", "other-login"),
                link("expected", "otp-2", "entry-1"),
            )

        val endpointTypes =
            mapOf(
                "entry-1" to EntryType.LOGIN,
                "other-login" to EntryType.LOGIN,
                "otp-1" to EntryType.OTP,
                "otp-2" to EntryType.OTP,
            )
        assertEquals("otp-2", findLinkedOtpEntryId("entry-1", links, endpointTypes))
        assertEquals(null, findLinkedOtpEntryId("missing", links, endpointTypes))
    }

    @Test
    fun absentOrBlankRequestedValuesDoNotProducePayloadFields() {
        val resolution =
            resolveAutofillCredential(
                entity = loginEntity(),
                requestedFields = setOf(AutofillCredentialField.USERNAME, AutofillCredentialField.PASSWORD),
                values = mapOf(AutofillCredentialField.USERNAME to " "),
            )

        assertSame(AutofillCredentialResolution.NotFillable, resolution)
    }

    private fun loginEntity(): EntryEntity =
        EntryEntity(
            entryId = "entry-1",
            entryType = EntryType.LOGIN,
            title = "Example",
            username = "person@example.com",
            createdAt = 10L,
            updatedAt = 20L,
        )

    private fun link(
        id: String,
        source: String,
        target: String,
    ): EntryLinkEntity {
        val endpoints = listOf(source, target).sorted()
        return EntryLinkEntity(
            linkId = id,
            firstEntryId = endpoints[0],
            secondEntryId = endpoints[1],
            createdAt = 10L,
            updatedAt = 10L,
        )
    }
}

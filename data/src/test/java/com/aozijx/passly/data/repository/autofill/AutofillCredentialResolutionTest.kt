package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
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

    @Test
    fun linkedOtpIsSelectedOnlyWhenItTargetsTheLogin() {
        val links = listOf(
            link("unrelated", "otp-1", "other-login"),
            link("expected", "otp-2", "entry-1"),
        )

        assertEquals("otp-2", findLinkedOtpEntryId("entry-1", links))
        assertEquals(null, findLinkedOtpEntryId("missing", links))
    }

    @Test
    fun otpCodeIsGeneratedOnlyWhenRequested() {
        val config = OtpConfig(secret = null)

        assertEquals(
            "282760",
            generateAutofillOtpCode(
                includeOtp = true,
                config = config,
                secret = "JBSWY3DPEHPK3PXP",
                timestampSeconds = 0L,
            ),
        )
        assertEquals(
            null,
            generateAutofillOtpCode(
                includeOtp = false,
                config = config,
                secret = "JBSWY3DPEHPK3PXP",
                timestampSeconds = 0L,
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

    private fun link(
        id: String,
        source: String,
        target: String,
    ): EntryLinkEntity = EntryLinkEntity(
        linkId = id,
        sourceEntryId = source,
        targetEntryId = target,
        relationType = EntryRelationType.OTP_FOR,
        createdAt = 10L,
        updatedAt = 10L,
    )
}

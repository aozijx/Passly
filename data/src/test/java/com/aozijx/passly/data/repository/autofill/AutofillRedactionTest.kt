package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillRedactionTest {
    @Test
    fun redactedLoginPreservesCredentialKind() {
        val secret = redactedSecretFor(EntryType.LOGIN)

        assertEquals(EntryCredentialKind.LOGIN, secret.credential.kind)
        Entry(
            identity = EntryIdentity(
                id = EntryId("entry"),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(createdAtMs = 1L),
            ),
            profile = EntryProfile(title = "Login"),
            secret = secret,
        )
    }
}

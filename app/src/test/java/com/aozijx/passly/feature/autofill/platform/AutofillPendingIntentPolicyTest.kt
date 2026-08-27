package com.aozijx.passly.feature.autofill.platform

import android.app.PendingIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillPendingIntentPolicyTest {
    @Test
    fun `activity pending intents remain mutable and update current`() {
        val flags = AutofillPendingIntentPolicy.ACTIVITY_FLAGS

        assertTrue(flags and PendingIntent.FLAG_MUTABLE != 0)
        assertTrue(flags and PendingIntent.FLAG_UPDATE_CURRENT != 0)
    }

    @Test
    fun `request identities preserve legacy and credential cancellation keys`() {
        assertEquals("autofill-fill:entry-1", AutofillPendingIntentPolicy.legacyFillRequestId("entry-1"))
        assertEquals("autofill-base:unknown", AutofillPendingIntentPolicy.legacyBaseRequestId(null))
        assertEquals("password-get:entry-1", AutofillPendingIntentPolicy.credentialGetRequestId("entry-1"))
        assertEquals("entry-1".hashCode(), AutofillPendingIntentPolicy.credentialGetRequestCode("entry-1"))
        assertNotEquals(
            AutofillPendingIntentPolicy.CREDENTIAL_CREATE_REQUEST_CODE,
            AutofillPendingIntentPolicy.CREDENTIAL_UNLOCK_REQUEST_CODE,
        )
    }
}

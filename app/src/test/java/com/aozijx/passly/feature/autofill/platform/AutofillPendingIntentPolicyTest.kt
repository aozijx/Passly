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
        assertEquals(
            "autofill-fill:interaction-1:entry-1",
            AutofillPendingIntentPolicy.legacyFillRequestId("interaction-1", "entry-1"),
        )
        assertEquals(
            "autofill-interaction:interaction-1",
            AutofillPendingIntentPolicy.legacyInteractionRequestId("interaction-1"),
        )
        assertEquals(
            "autofill-save:opaque-token",
            AutofillPendingIntentPolicy.legacySaveRequestId("opaque-token"),
        )
        assertNotEquals(
            AutofillPendingIntentPolicy.legacySaveRequestId("interaction-1"),
            AutofillPendingIntentPolicy.legacyInteractionRequestId("interaction-1"),
        )
        assertEquals(
            "password-get:interaction-1:entry-1",
            AutofillPendingIntentPolicy.credentialGetRequestId("interaction-1", "entry-1"),
        )
        assertEquals(
            "password-get:interaction-1:entry-1".hashCode(),
            AutofillPendingIntentPolicy.credentialGetRequestCode("interaction-1", "entry-1"),
        )
        assertNotEquals(
            AutofillPendingIntentPolicy.credentialCreateRequestId("interaction-1"),
            AutofillPendingIntentPolicy.credentialUnlockRequestId("interaction-1"),
        )
        assertNotEquals(
            AutofillPendingIntentPolicy.legacySaveRequestId("interaction-1"),
            AutofillPendingIntentPolicy.credentialCreateRequestId("interaction-1"),
        )
    }
}

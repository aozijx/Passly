package com.aozijx.passly.feature.autofill.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillLaunchExtrasTest {
    @Test
    fun `credential provider actions remain stable launch contract values`() {
        assertEquals("com.aozijx.passly.ACTION_GET_PASSWORD", AutofillLaunchExtras.ACTION_CREDENTIAL_GET_PASSWORD)
        assertEquals("com.aozijx.passly.ACTION_CREDENTIAL_UNLOCK", AutofillLaunchExtras.ACTION_CREDENTIAL_UNLOCK)
        assertEquals("com.aozijx.passly.ACTION_CREATE_PASSWORD", AutofillLaunchExtras.ACTION_CREDENTIAL_CREATE_PASSWORD)
    }
}

package com.aozijx.passly.core.platform.path

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UriDisplayNameFormatterTest {
    @Test fun `uses final uri segment as display name`() {
        assertEquals("backup.passly", UriDisplayNameFormatter.format("content://vault/tree/backup.passly"))
    }

    @Test fun `truncates long display name while preserving its extension`() {
        val result =
            UriDisplayNameFormatter.format(
                "content://vault/tree/backup-directory-name-that-is-too-long.passly",
            )

        assertEquals(32, result?.length)
        assertTrue(result!!.endsWith("...passly"))
    }
}

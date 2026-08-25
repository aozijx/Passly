package com.aozijx.passly.core.platform.path

import org.junit.Assert.assertEquals
import org.junit.Test

class UriDisplayNameFormatterTest {
    @Test fun `uses final uri segment as display name`() {
        assertEquals("backup.passly", UriDisplayNameFormatter.format("content://vault/tree/backup.passly"))
    }
}

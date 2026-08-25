package com.aozijx.passly.core.platform.cache

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteSizeFormatterTest {
    @Test fun `formats binary units with one decimal`() {
        assertEquals("512 B", ByteSizeFormatter.format(512))
        assertEquals("1.5 KB", ByteSizeFormatter.format(1536))
        assertEquals("2.0 MB", ByteSizeFormatter.format(2L * 1024 * 1024))
    }
}

package com.aozijx.passly.core.platform.cache

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteSizeFormatterTest {
    @Test fun `formats binary units with one decimal`() {
        assertEquals("0 B", ByteSizeFormatter.format(0))
        assertEquals("512 B", ByteSizeFormatter.format(512))
        assertEquals("1023 B", ByteSizeFormatter.format(1023))
        assertEquals("1.0 KB", ByteSizeFormatter.format(1024))
        assertEquals("1.5 KB", ByteSizeFormatter.format(1536))
        assertEquals("1.0 MB", ByteSizeFormatter.format(1024L * 1024))
        assertEquals("2.0 MB", ByteSizeFormatter.format(2L * 1024 * 1024))
        assertEquals("1.0 GB", ByteSizeFormatter.format(1024L * 1024 * 1024))
    }
}

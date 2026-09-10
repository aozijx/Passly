package com.aozijx.passly.core.ui.components.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownDocumentPolicyTest {

    @Test
    fun `normalization removes outer whitespace and preserves markdown structure`() {
        assertEquals(
            "# Title\n\n- first\n- second",
            normalizeMarkdownDocument("  \n# Title\n\n- first\n- second\n  "),
        )
    }

    @Test
    fun `blank content delegates to the empty state`() {
        assertNull(normalizeMarkdownDocument(" \n\t "))
        assertNull(normalizeMarkdownDocument(null))
    }
}

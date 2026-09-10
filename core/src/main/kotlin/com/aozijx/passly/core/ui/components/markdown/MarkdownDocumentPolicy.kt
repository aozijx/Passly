package com.aozijx.passly.core.ui.components.markdown

internal fun normalizeMarkdownDocument(content: String?): String? =
    content?.trim()?.takeIf(String::isNotEmpty)

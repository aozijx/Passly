package com.aozijx.passly.core.ui.components.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders markdown-like document content with Passly's app-level defaults.
 *
 * This component intentionally owns the document rendering policy: whitespace
 * normalization, selectable text, empty-state delegation, and typography mapping.
 * Keep feature screens focused on when to show notes, not how markdown is styled.
 */
@Composable
fun PasslyMarkdownDocument(
    content: String?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null
) {
    val normalizedContent = normalizeMarkdownDocument(content)

    Box(modifier = modifier) {
        if (normalizedContent == null) {
            emptyContent?.invoke()
        } else {
            MarkwonDocumentView(content = normalizedContent, onLongClick = onLongClick)
        }
    }
}

package com.aozijx.passly.core.ui.components.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

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
    emptyContent: (@Composable () -> Unit)? = null
) {
    val normalizedContent = remember(content) { content?.trim().orEmpty() }

    Box(modifier = modifier) {
        if (normalizedContent.isBlank()) {
            emptyContent?.invoke()
        } else {
            SelectionContainer {
                Markdown(
                    content = normalizedContent,
                    typography = passlyMarkdownTypography()
                )
            }
        }
    }
}

@Composable
private fun passlyMarkdownTypography() = markdownTypography(
    h1 = MaterialTheme.typography.headlineLarge,
    h2 = MaterialTheme.typography.headlineMedium,
    h3 = MaterialTheme.typography.headlineSmall,
    h4 = MaterialTheme.typography.titleLarge,
    h5 = MaterialTheme.typography.titleMedium,
    h6 = MaterialTheme.typography.titleSmall,
    text = MaterialTheme.typography.bodyMedium,
    paragraph = MaterialTheme.typography.bodyMedium,
    ordered = MaterialTheme.typography.bodyMedium,
    bullet = MaterialTheme.typography.bodyMedium,
    list = MaterialTheme.typography.bodyMedium
)

package com.aozijx.passly.core.ui.components.markdown

import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

@Composable
internal fun MarkwonDocumentView(
    content: String,
    modifier: Modifier = Modifier,
) {
    val applicationContext = LocalContext.current.applicationContext
    val markwon = remember(applicationContext) { createMarkwon(applicationContext) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val textSizeSp = MaterialTheme.typography.bodyMedium.fontSize.value

    AndroidView(
        factory = { context -> PasslyMarkdownTextView(context) },
        modifier = modifier.fillMaxWidth(),
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            if (textView.renderedSource != content) {
                markwon.setMarkdown(textView, content)
                textView.renderedSource = content
            }
        },
    )
}

private fun createMarkwon(context: Context): Markwon =
    Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .build()

private class PasslyMarkdownTextView(context: Context) : TextView(context) {
    var renderedSource: String? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        includeFontPadding = false
        movementMethod = LinkMovementMethod.getInstance()
        setPadding(0, 0, 0, 0)
        setTextIsSelectable(true)
    }
}

package com.example.poop.ui.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding

@Composable
fun LogDetailDialog(
    isVisible: Boolean,
    isLoading: Boolean,
    content: String?,
    error: String?,
    title: String = "日志详情",
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    } else if (content != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val vScroll = rememberScrollState()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(vScroll)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Markdown(
                                    content = content,
                                    modifier = Modifier.fillMaxWidth(),
                                    typography = markdownTypography(
                                        h1 = MaterialTheme.typography.titleLarge,
                                        h2 = MaterialTheme.typography.titleMedium,
                                        h3 = MaterialTheme.typography.titleSmall,
                                        text = MaterialTheme.typography.bodyMedium,
                                    ),
                                    padding = markdownPadding(
                                        block = 4.dp,
                                        list = 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        )
    }
}
package com.aozijx.passly.features.settings.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.SwipeActionType

@Composable
fun SwipeActionSelectDialog(
    title: String,
    currentAction: SwipeActionType,
    onActionSelected: (SwipeActionType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                SwipeActionType.entries.forEach { action ->
                    val isSelected = action == currentAction
                    val selectedBackground = when (action) {
                        SwipeActionType.DELETE -> MaterialTheme.colorScheme.errorContainer
                        SwipeActionType.COPY_PASSWORD -> MaterialTheme.colorScheme.secondaryContainer
                        SwipeActionType.EDIT -> MaterialTheme.colorScheme.tertiaryContainer
                        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.primaryContainer
                        SwipeActionType.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val selectedContentColor = when (action) {
                        SwipeActionType.DELETE -> MaterialTheme.colorScheme.onErrorContainer
                        SwipeActionType.COPY_PASSWORD -> MaterialTheme.colorScheme.onSecondaryContainer
                        SwipeActionType.EDIT -> MaterialTheme.colorScheme.onTertiaryContainer
                        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.onPrimaryContainer
                        SwipeActionType.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) selectedBackground else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { onActionSelected(action) })
                        Spacer(modifier = Modifier.width(8.dp))
                        action.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = action.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(28.dp)
    )
}

package com.aozijx.passly.presentation.feature.settings.main.interaction

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.menu.MenuOptionText
import com.aozijx.passly.domain.settings.model.SwipeActionType

private val SWIPE_ACTIONS = listOf(
    SwipeActionType.DELETE,
    SwipeActionType.DETAIL,
    SwipeActionType.COPY_PASSWORD,
    SwipeActionType.COPY_USERNAME
)

private fun SwipeActionType.icon(): ImageVector? = when (this) {
    SwipeActionType.DELETE -> Icons.Default.Delete
    SwipeActionType.DETAIL -> Icons.Default.Info
    SwipeActionType.COPY_PASSWORD -> Icons.Default.ContentCopy
    SwipeActionType.COPY_USERNAME -> Icons.Default.Person
}

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
                SWIPE_ACTIONS.forEach { action ->
                    val isSelected = action == currentAction
                    val selectedBackground = when (action) {
                        SwipeActionType.DELETE -> MaterialTheme.colorScheme.errorContainer
                        SwipeActionType.COPY_PASSWORD, SwipeActionType.COPY_USERNAME -> MaterialTheme.colorScheme.secondaryContainer
                        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val selectedContentColor = when (action) {
                        SwipeActionType.DELETE -> MaterialTheme.colorScheme.onErrorContainer
                        SwipeActionType.COPY_PASSWORD, SwipeActionType.COPY_USERNAME -> MaterialTheme.colorScheme.onSecondaryContainer
                        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) selectedBackground else Color.Transparent,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { onActionSelected(action) })
                        Spacer(modifier = Modifier.width(8.dp))
                        action.icon()?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        MenuOptionText(
                            text = action.localizedLabel(),
                            selected = isSelected,
                            style = MaterialTheme.typography.bodyLarge,
                            selectedColor = selectedContentColor,
                            unselectedColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

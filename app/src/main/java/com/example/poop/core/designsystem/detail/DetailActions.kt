package com.example.poop.core.designsystem.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.poop.core.common.SwipeActionType
import com.example.poop.data.local.AppPrefs

@Composable
fun DetailActions(
    onDeleteClick: () -> Unit,
    showDelete: Boolean = true
) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val isSwipeEnabled by prefs.isSwipeEnabled.collectAsState(initial = true)
    val swipeLeftAction by prefs.swipeLeftAction.collectAsState(initial = SwipeActionType.DELETE)
    val swipeRightAction by prefs.swipeRightAction.collectAsState(initial = SwipeActionType.DISABLED)
    val shouldShowDelete = showDelete && (!isSwipeEnabled || (swipeLeftAction != SwipeActionType.DELETE && swipeRightAction != SwipeActionType.DELETE))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (shouldShowDelete) {
            OutlinedButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("删除")
            }
        }
    }
}

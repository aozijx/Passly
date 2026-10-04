package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.RelatedEntryUiModel
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
fun RelatedEntriesSection(
    entries: List<RelatedEntryUiModel>,
    onOpenEntry: (String) -> Unit
) {
    if (entries.isEmpty()) return
    InfoGroupCard(title = stringResource(R.string.related_entries)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEntry(entry.id) }
                        .padding(horizontal = 4.dp),
                    leadingContent = null,
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    overlineContent = null,
                    supportingContent = { Text(stringResource(entry.entryType.labelRes)) },
                    colors = ListItemDefaults.colors(),
                    elevation = ListItemDefaults.elevation(),
                    content = { Text(entry.title) },
                )
                if (index < entries.lastIndex) HorizontalDivider()
            }
        }
    }
}

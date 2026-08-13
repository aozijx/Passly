package com.aozijx.passly.feature.detail.ui.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.text.localizedName
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.detail.ui.components.InfoGroupCard

@Composable
fun RelatedEntriesSection(
    entries: List<Entry>,
    onOpenEntry: (Entry) -> Unit
) {
    if (entries.isEmpty()) return
    InfoGroupCard(title = stringResource(R.string.related_entries)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                ListItem(
                    headlineContent = { Text(entry.title) },
                    supportingContent = { Text(entry.type.localizedName()) },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEntry(entry) }
                        .padding(horizontal = 4.dp)
                )
                if (index < entries.lastIndex) HorizontalDivider()
            }
        }
    }
}

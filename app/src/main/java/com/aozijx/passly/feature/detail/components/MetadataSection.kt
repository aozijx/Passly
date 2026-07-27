package com.aozijx.passly.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.VaultEntry
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun MetadataSection(entry: VaultEntry) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("yyyy-MM-dd HH:mm", locale) }
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MetadataText(
            stringResource(R.string.metadata_created_at, dateFormat.format(Date(entry.createdAt)))
        )
        MetadataText(
            stringResource(
                R.string.metadata_last_modified,
                dateFormat.format(Date(entry.updatedAt))
            )
        )
    }
}

@Composable
fun MetadataText(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline
    )
}

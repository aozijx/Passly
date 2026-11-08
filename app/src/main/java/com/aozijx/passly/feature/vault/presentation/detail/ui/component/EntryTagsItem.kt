package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagChipsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTagsItem(
    tags: DetailTagChipsUiState,
    onClick: () -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (tags) {
            DetailTagChipsUiState.Empty -> {
                Text(
                    text = stringResource(R.string.vault_detail_tags_add),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is DetailTagChipsUiState.Items -> {
                tags.tags.forEach { tag ->
                    AssistChip(onClick = onClick, label = { Text(tag) })
                }
            }
        }
    }
}

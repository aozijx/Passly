package com.aozijx.passly.feature.autofill.presentation.legacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.PasslySheetTitle
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.autofill.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillCandidateBottomSheet(
    candidates: List<AutofillCandidateSummary>,
    entryIcon: @Composable (EntryListItem, Modifier) -> Unit,
    onCandidateSelected: (EntryId) -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    PasslySheetTitle(text = stringResource(R.string.autofill_candidates_title))
                    Text(
                        text =
                            pluralStringResource(
                                id = R.plurals.autofill_entry_compact_summary,
                                count = candidates.size,
                                candidates.size,
                            ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(
                items = candidates,
                key = { candidate -> candidate.entry.id.value },
            ) { candidate ->
                CandidateItem(
                    candidate = candidate,
                    entryIcon = entryIcon,
                    onClick = { onCandidateSelected(candidate.entry.id) },
                )
            }
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: AutofillCandidateSummary,
    entryIcon: @Composable (EntryListItem, Modifier) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entryIcon(candidate.entry, Modifier)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = candidate.entry.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

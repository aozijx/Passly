package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel

@Composable
internal fun DetailRelatedEntriesHost(
    entries: List<Entry>,
    models: List<RelatedEntryUiModel>,
    onOpenEntry: (Entry) -> Unit,
) {
    RelatedEntriesSection(
        entries = models,
        onOpenEntry = { id -> entries.firstOrNull { it.id.value == id }?.let(onOpenEntry) },
    )
}

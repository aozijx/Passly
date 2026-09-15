package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryTagsItem
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard

@Composable
internal fun DetailTagsBinding(entry: Entry, onAction: (DetailUiAction) -> Unit) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
        EntryTagsItem(
            tags = entry.tags,
            onClick = { onAction(DetailUiAction.OpenTagEditor) },
        )
    }
}

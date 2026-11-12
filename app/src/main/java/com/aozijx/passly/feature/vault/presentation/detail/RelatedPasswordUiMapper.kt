package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.query.EntryListItem

internal fun EntryListItem.toCandidateUiModel(): RelatedPasswordCandidateUiModel =
    RelatedPasswordCandidateUiModel(
        id = id.value,
        title = title,
        username =
            username
                .takeIf(String::isNotBlank)
                ?.let(RelatedPasswordCandidateUsernameUiState::Visible)
                ?: RelatedPasswordCandidateUsernameUiState.Hidden,
    )

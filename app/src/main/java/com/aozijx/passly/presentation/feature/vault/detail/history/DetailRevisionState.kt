package com.aozijx.passly.presentation.feature.vault.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata

enum class DetailRevisionDestination { LIST, COMPARISON }

enum class DetailRevisionFailure { MISSING, STALE, INVALID, AUTHENTICATION, UNEXPECTED }

data class DetailRevisionState(
    val visible: Boolean = false,
    val entryId: EntryId? = null,
    val destination: DetailRevisionDestination = DetailRevisionDestination.LIST,
    val metadata: List<EntryRevisionMetadata> = emptyList(),
    val selectedRevisionId: String? = null,
    val differences: List<EntryRevisionDifference> = emptyList(),
    val loading: Boolean = false,
    val confirmRestore: Boolean = false,
    val restoring: Boolean = false,
    val failure: DetailRevisionFailure? = null,
)

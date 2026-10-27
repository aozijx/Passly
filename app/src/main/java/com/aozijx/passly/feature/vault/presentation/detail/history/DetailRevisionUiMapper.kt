package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.asScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionRestoreUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionSheetUiModel

internal fun DetailRevisionState.toRevisionSheetUiModel(
    reveals: Map<SensitiveFieldKey, SensitiveValue>,
): DetailRevisionSheetUiModel = when (this) {
    DetailRevisionState.Closed -> DetailRevisionSheetUiModel.Hidden
    is DetailRevisionState.Open -> when (val currentPage = page) {
        DetailRevisionPage.List -> DetailRevisionSheetUiModel.ListPage(
            content = revisions.toUiModel(),
        )
        is DetailRevisionPage.Comparison -> DetailRevisionSheetUiModel.ComparisonPage(
            revisionId = currentPage.revisionId,
            content = currentPage.phase.toUiModel(reveals),
        )
    }
}

private fun DetailRevisionListState.toUiModel(): DetailRevisionListUiState = when (this) {
    DetailRevisionListState.Loading -> DetailRevisionListUiState.Loading
    is DetailRevisionListState.Failed -> DetailRevisionListUiState.Failed
    is DetailRevisionListState.Ready -> DetailRevisionListUiState.Ready(
        revisions = revisions.map {
            DetailRevisionItemUiModel(
                id = it.id.value,
                version = it.version.value,
                createdAtMs = it.createdAtMs,
                change = it.change,
            )
        },
    )
}

private fun DetailRevisionComparisonPhase.toUiModel(
    reveals: Map<SensitiveFieldKey, SensitiveValue>,
): DetailRevisionComparisonUiState = when (this) {
    DetailRevisionComparisonPhase.Loading -> DetailRevisionComparisonUiState.Loading
    is DetailRevisionComparisonPhase.Failed -> DetailRevisionComparisonUiState.Failed
    is DetailRevisionComparisonPhase.Ready -> DetailRevisionComparisonUiState.Ready(
        differences = differences
            .filter { it.kind != RevisionDifferenceKind.UNCHANGED }
            .map { difference ->
                val sensitiveKey = (difference.field as? RevisionFieldId.Sensitive)?.key
                DetailRevisionDifferenceUiModel(
                    field = difference.field,
                    kind = difference.kind,
                    before = difference.before,
                    after = difference.after,
                    revealedValue = sensitiveKey?.let(reveals::get)?.asScopedSensitiveText(),
                )
            },
        revealableKeys = differences.mapNotNullTo(linkedSetOf()) {
            (it.field as? RevisionFieldId.Sensitive)?.key
        },
        restore = when (restore) {
            DetailRevisionRestorePhase.Idle -> DetailRevisionRestoreUiState.Idle
            DetailRevisionRestorePhase.Confirming -> DetailRevisionRestoreUiState.Confirming
            DetailRevisionRestorePhase.Restoring -> DetailRevisionRestoreUiState.Restoring
        },
    )
}


package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.asScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionSheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel

internal fun DetailRevisionState.toRevisionSheetUiModel(reveals: Map<SensitiveFieldKey, SensitiveValue>): DetailRevisionSheetUiModel =
    when (this) {
        DetailRevisionState.Closed -> {
            DetailRevisionSheetUiModel.Hidden
        }

        is DetailRevisionState.Open -> {
            when (val currentPage = page) {
                DetailRevisionPage.List -> {
                    DetailRevisionSheetUiModel.ListPage(
                        content = revisions.toUiModel(),
                    )
                }

                is DetailRevisionPage.Comparison -> {
                    DetailRevisionSheetUiModel.ComparisonPage(
                        revisionId = currentPage.revisionId,
                        content = currentPage.phase.toUiModel(reveals),
                    )
                }
            }
        }
    }

private fun DetailRevisionListState.toUiModel(): DetailRevisionListUiState =
    when (this) {
        DetailRevisionListState.Loading -> {
            DetailRevisionListUiState.Loading
        }

        is DetailRevisionListState.Failed -> {
            DetailRevisionListUiState.Failed
        }

        is DetailRevisionListState.Ready -> {
            val items =
                revisions.map {
                    DetailRevisionItemUiModel(
                        id = it.id.value,
                        version = it.version.value,
                        createdAtMs = it.createdAtMs,
                        change = it.change,
                    )
                }
            items
                .takeIf { it.isNotEmpty() }
                ?.let(DetailRevisionListUiState::ReadyItems)
                ?: DetailRevisionListUiState.ReadyEmpty
        }
    }

private fun DetailRevisionComparisonPhase.toUiModel(reveals: Map<SensitiveFieldKey, SensitiveValue>): DetailRevisionComparisonUiState =
    when (this) {
        DetailRevisionComparisonPhase.Loading -> {
            DetailRevisionComparisonUiState.Loading
        }

        is DetailRevisionComparisonPhase.Failed -> {
            DetailRevisionComparisonUiState.Failed
        }

        is DetailRevisionComparisonPhase.Ready -> {
            DetailRevisionComparisonUiState.Ready(
                differences =
                    differences
                        .filter { it.kind != RevisionDifferenceKind.UNCHANGED }
                        .map { difference ->
                            val sensitiveKey = (difference.field as? RevisionFieldId.Sensitive)?.key
                            DetailRevisionDifferenceUiModel(
                                field = difference.field,
                                kind = difference.kind,
                                before = difference.before?.toUiModel() ?: DetailRevisionValueUiModel.Absent,
                                after = difference.after?.toUiModel() ?: DetailRevisionValueUiModel.Absent,
                                revealedValue = sensitiveKey?.let(reveals::get)?.asScopedSensitiveText(),
                            )
                        },
                restore = restore,
            )
        }
    }

private fun RevisionFieldValue.toUiModel(): DetailRevisionValueUiModel =
    when (this) {
        is RevisionFieldValue.Text -> DetailRevisionValueUiModel.Text(value)
        is RevisionFieldValue.TextSet -> DetailRevisionValueUiModel.Text(values.sorted().joinToString())
        is RevisionFieldValue.Flag -> DetailRevisionValueUiModel.Flag(value)
        is RevisionFieldValue.EpochMillis -> DetailRevisionValueUiModel.EpochMillis(value)
        is RevisionFieldValue.Icon ->
            if (value.customReference.isNullOrBlank()) {
                DetailRevisionValueUiModel.Icon.DEFAULT
            } else {
                DetailRevisionValueUiModel.Icon.CUSTOM
            }
        is RevisionFieldValue.Links -> DetailRevisionValueUiModel.Links(values.size)
        is RevisionFieldValue.AttachmentIds -> DetailRevisionValueUiModel.Attachments(values.size)
    }

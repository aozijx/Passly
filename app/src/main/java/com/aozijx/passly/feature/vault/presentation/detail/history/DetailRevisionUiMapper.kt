package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.history.EntryRevisionSummary
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.labelRes
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryPhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryState
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.asScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryAffectedFieldsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryLoadState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryRevisionReference
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionFieldLabelUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionRevealActionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel

internal fun DetailRevisionState.toHistorySheetUiModel(
    filter: DetailHistoryFilter,
    activityHistory: DetailActivityHistoryState,
    activityItems: List<DetailHistoryItemUiModel>,
    reveals: Map<SensitiveFieldKey, SensitiveValue>,
): DetailHistorySheetUiModel =
    when (this) {
        DetailRevisionState.Closed -> {
            DetailHistorySheetUiModel.Hidden
        }

        is DetailRevisionState.Open -> {
            when (val currentPage = page) {
                DetailRevisionPage.List -> {
                    DetailHistorySheetUiModel.ListPage(
                        content = toHistoryListUiState(filter, activityHistory, activityItems, revisions),
                    )
                }

                is DetailRevisionPage.Comparison -> {
                    DetailHistorySheetUiModel.ComparisonPage(
                        revisionId = currentPage.revisionId,
                        content = currentPage.phase.toUiModel(reveals),
                    )
                }
            }
        }
    }

private fun toHistoryListUiState(
    filter: DetailHistoryFilter,
    activityHistory: DetailActivityHistoryState,
    activityItems: List<DetailHistoryItemUiModel>,
    revisionState: DetailRevisionListState,
): DetailHistoryListUiState {
    val revisionItems =
        (revisionState as? DetailRevisionListState.Ready)
            ?.revisions
            .orEmpty()
            .map(EntryRevisionSummary::toUiModel)
    val visibleRevisionItems = revisionItems.drop(1)
    val items =
        mergeHistoryItems(activityItems, revisionItems, visibleRevisionItems)
            .filter { filter == DetailHistoryFilter.ALL || it.filter == filter }
    val activitySource =
        when (activityHistory) {
            DetailActivityHistoryState.Loading -> DetailHistoryLoadState.LOADING
            DetailActivityHistoryState.Failed -> DetailHistoryLoadState.FAILED
            is DetailActivityHistoryState.Content ->
                when (activityHistory.phase) {
                    DetailActivityHistoryPhase.Refreshing -> DetailHistoryLoadState.LOADING
                    DetailActivityHistoryPhase.Ready -> DetailHistoryLoadState.READY
                    DetailActivityHistoryPhase.Failed -> DetailHistoryLoadState.FAILED
                }
        }
    val revisionSource =
        when (revisionState) {
            DetailRevisionListState.Loading -> DetailHistoryLoadState.LOADING
            is DetailRevisionListState.Failed -> DetailHistoryLoadState.FAILED
            is DetailRevisionListState.Ready -> DetailHistoryLoadState.READY
        }
    return DetailHistoryListUiState(
        filter = filter,
        items = items,
        activitySource = activitySource,
        revisionSource = revisionSource,
    )
}

private fun EntryRevisionSummary.toUiModel(): DetailRevisionItemUiModel =
    DetailRevisionItemUiModel(
        id = metadata.id.value,
        version = metadata.version.value - EntryVersion.INITIAL.value,
        storedVersion = metadata.version.value,
        createdAtMs = metadata.createdAtMs,
        change = metadata.change,
        changedFields = changedFields.map(RevisionFieldId::toUiLabel),
    )

private fun mergeHistoryItems(
    activities: List<DetailHistoryItemUiModel>,
    allRevisionItems: List<DetailRevisionItemUiModel>,
    visibleRevisionItems: List<DetailRevisionItemUiModel>,
): List<DetailHistoryItemUiModel> {
    val revisionsByVersion = allRevisionItems.associateBy(DetailRevisionItemUiModel::storedVersion)
    val visibleRevisionVersions = visibleRevisionItems.mapTo(mutableSetOf(), DetailRevisionItemUiModel::storedVersion)
    val matchedVersions = mutableSetOf<Int>()
    val eventItems =
        activities.map { activity ->
            val pendingVersion = (activity.revision as? DetailHistoryRevisionReference.Pending)?.version
            val matchingRevision = pendingVersion?.let(revisionsByVersion::get)
            matchingRevision?.storedVersion?.let(matchedVersions::add)
            if (matchingRevision == null) {
                activity
            } else {
                activity.copy(
                    affectedFields =
                        (activity.affectedFields.labelsOrEmpty() + matchingRevision.changedFields)
                            .distinct()
                            .toAffectedFieldsUiState(),
                    revision =
                        if (matchingRevision.storedVersion in visibleRevisionVersions) {
                            DetailHistoryRevisionReference.Linked(matchingRevision.storedVersion, matchingRevision.id)
                        } else {
                            DetailHistoryRevisionReference.Unlinked
                        },
                )
            }
        }
    val standaloneRevisions =
        visibleRevisionItems
            .asSequence()
            .filterNot { it.storedVersion in matchedVersions }
            .map { revision ->
                DetailHistoryItemUiModel(
                    id = "revision:${revision.id}",
                    description =
                        DetailHistoryDescriptionUiState.Snapshot(
                            version = revision.version,
                            changeLabelRes =
                                if (revision.change == RevisionChange.VERSION_RESTORED) {
                                    R.string.vault_detail_activity_version_restored
                                } else {
                                    R.string.revision_change_changed
                                },
                        ),
                    icon =
                        if (revision.change == RevisionChange.VERSION_RESTORED) {
                            DetailHistoryIcon.VERSION_RESTORED
                        } else {
                            DetailHistoryIcon.SNAPSHOT
                        },
                    filter = DetailHistoryFilter.CONTENT_CHANGES,
                    affectedFields = revision.changedFields.toAffectedFieldsUiState(),
                    createdAtMs = revision.createdAtMs,
                    revision = DetailHistoryRevisionReference.Linked(revision.storedVersion, revision.id),
                )
            }
    return (eventItems + standaloneRevisions)
        .sortedWith(compareByDescending<DetailHistoryItemUiModel> { it.createdAtMs }.thenBy { it.id })
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
            val differences =
                differences
                    .filter { it.kind != RevisionDifferenceKind.UNCHANGED }
                    .map { difference ->
                        val sensitiveKey = (difference.field as? RevisionFieldId.Sensitive)?.key
                        val content =
                            when (val field = difference.field) {
                                is RevisionFieldId.Sensitive -> {
                                    reveals[field.key]
                                        ?.asScopedSensitiveText()
                                        ?.let(DetailRevisionDifferenceContentUiState::ProtectedRevealed)
                                        ?: DetailRevisionDifferenceContentUiState.ProtectedHidden
                                }

                                is RevisionFieldId.ProtectedField,
                                is RevisionFieldId.ProtectedCustomField,
                                -> {
                                    DetailRevisionDifferenceContentUiState.ProtectedHidden
                                }

                                else -> {
                                    DetailRevisionDifferenceContentUiState.Values(
                                        before = difference.before?.toUiModel() ?: DetailRevisionValueUiModel.Absent,
                                        after = difference.after?.toUiModel() ?: DetailRevisionValueUiModel.Absent,
                                    )
                                }
                            }
                        DetailRevisionDifferenceUiModel(
                            field = difference.field,
                            label = difference.field.toUiLabel(),
                            changeLabelRes = difference.kind.toUiLabelRes(),
                            content = content,
                            revealAction =
                                sensitiveKey
                                    ?.takeUnless { difference.kind == RevisionDifferenceKind.ADDED }
                                    ?.let(DetailRevisionRevealActionUiState::Available)
                                    ?: DetailRevisionRevealActionUiState.Unavailable,
                        )
                    }
            DetailRevisionComparisonUiState.Ready(
                content =
                    differences
                        .takeIf { it.isNotEmpty() }
                        ?.let(DetailRevisionComparisonContentUiState::Changes)
                        ?: DetailRevisionComparisonContentUiState.NoChanges,
                restore = restore,
            )
        }
    }

private fun DetailHistoryAffectedFieldsUiState.labelsOrEmpty(): List<DetailRevisionFieldLabelUiState> =
    when (this) {
        DetailHistoryAffectedFieldsUiState.None -> emptyList()
        is DetailHistoryAffectedFieldsUiState.Labels -> items
    }

private fun List<DetailRevisionFieldLabelUiState>.toAffectedFieldsUiState(): DetailHistoryAffectedFieldsUiState =
    takeIf { it.isNotEmpty() }
        ?.let(DetailHistoryAffectedFieldsUiState::Labels)
        ?: DetailHistoryAffectedFieldsUiState.None

internal fun RevisionFieldId.toUiLabel(): DetailRevisionFieldLabelUiState =
    when (this) {
        RevisionFieldId.Title -> {
            fieldLabel(FieldKey.TITLE)
        }

        RevisionFieldId.Username -> {
            fieldLabel(FieldKey.USERNAME)
        }

        RevisionFieldId.PrimaryUrl -> {
            fieldLabel(FieldKey.PRIMARY_URL)
        }

        RevisionFieldId.Domains -> {
            fieldLabel(FieldKey.DOMAINS)
        }

        RevisionFieldId.ApplicationIds -> {
            fieldLabel(FieldKey.APPLICATION_IDS)
        }

        RevisionFieldId.Icon -> {
            DetailRevisionFieldLabelUiState.Resource(R.string.revision_field_icon)
        }

        RevisionFieldId.Favorite -> {
            fieldLabel(FieldKey.FAVORITE)
        }

        RevisionFieldId.Tags -> {
            fieldLabel(FieldKey.TAGS)
        }

        RevisionFieldId.ExpiresAt -> {
            fieldLabel(FieldKey.EXPIRES_AT)
        }

        is RevisionFieldId.CredentialField -> {
            fieldLabel(key)
        }

        is RevisionFieldId.ProtectedField -> {
            fieldLabel(key)
        }

        RevisionFieldId.Notes -> {
            fieldLabel(FieldKey.NOTES)
        }

        is RevisionFieldId.CustomField -> {
            DetailRevisionFieldLabelUiState.Named(R.string.revision_custom_field_label, name)
        }

        is RevisionFieldId.ProtectedCustomField -> {
            DetailRevisionFieldLabelUiState.Named(R.string.revision_custom_field_label, name)
        }

        RevisionFieldId.Links -> {
            DetailRevisionFieldLabelUiState.Resource(R.string.revision_field_links)
        }

        RevisionFieldId.Attachments -> {
            DetailRevisionFieldLabelUiState.Resource(R.string.revision_field_attachments)
        }

        is RevisionFieldId.Sensitive -> {
            fieldLabel(key.toFieldKey())
        }
    }

private fun fieldLabel(fieldKey: FieldKey) = DetailRevisionFieldLabelUiState.Resource(fieldKey.labelRes())

private fun SensitiveFieldKey.toFieldKey(): FieldKey =
    when (this) {
        SensitiveFieldKey.PASSWORD -> FieldKey.PASSWORD
        SensitiveFieldKey.CARD_NUMBER -> FieldKey.CARD_NUMBER
        SensitiveFieldKey.CARD_CVV -> FieldKey.CARD_CVV
        SensitiveFieldKey.CARD_PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        SensitiveFieldKey.IDENTITY_NUMBER -> FieldKey.ID_NUMBER
        SensitiveFieldKey.SEED_PHRASE -> FieldKey.SEED_PHRASE
        SensitiveFieldKey.RECOVERY_CODES -> FieldKey.RECOVERY_CODES
        SensitiveFieldKey.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        SensitiveFieldKey.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE -> FieldKey.PASSKEY_DATA
        SensitiveFieldKey.OTP_SECRET -> FieldKey.OTP_SECRET
    }

private fun RevisionDifferenceKind.toUiLabelRes(): Int =
    when (this) {
        RevisionDifferenceKind.ADDED -> R.string.revision_change_added
        RevisionDifferenceKind.REMOVED -> R.string.revision_change_removed
        RevisionDifferenceKind.CHANGED -> R.string.revision_change_changed
        RevisionDifferenceKind.UNCHANGED -> R.string.revision_change_unchanged
    }

private fun RevisionFieldValue.toUiModel(): DetailRevisionValueUiModel =
    when (this) {
        is RevisionFieldValue.Text -> {
            DetailRevisionValueUiModel.Text(value)
        }

        is RevisionFieldValue.TextSet -> {
            DetailRevisionValueUiModel.Text(values.sorted().joinToString())
        }

        is RevisionFieldValue.Flag -> {
            DetailRevisionValueUiModel.Flag(value)
        }

        is RevisionFieldValue.EpochMillis -> {
            DetailRevisionValueUiModel.EpochMillis(value)
        }

        is RevisionFieldValue.Icon -> {
            if (value.customReference.isNullOrBlank()) {
                DetailRevisionValueUiModel.Icon.DEFAULT
            } else {
                DetailRevisionValueUiModel.Icon.CUSTOM
            }
        }

        is RevisionFieldValue.Links -> {
            DetailRevisionValueUiModel.Links(values.size)
        }

        is RevisionFieldValue.AttachmentIds -> {
            DetailRevisionValueUiModel.Attachments(values.size)
        }
    }

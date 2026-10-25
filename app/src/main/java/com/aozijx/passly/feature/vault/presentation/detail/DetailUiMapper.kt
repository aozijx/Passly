package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionComparisonPhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionListState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionPage
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.mapping.entry.toUiModel
import com.aozijx.passly.presentation.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.CredentialFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.CredentialSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailActivityTypeUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailActivityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailContentUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailHeaderUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailIconCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailMetadataUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailNotesUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailOtpUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailPresentationModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSectionUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionRestoreUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionSheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailTagEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedEntryUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.ScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.TagEditorValidationErrorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.TagEditorOverlayUiState

internal fun detailHeaderUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailHeaderUiModel(
    title = entry.detailDisplayTitle,
    favorite = entry.favorite,
    editedTitle = state.editedTitle,
    isEditingTitle = state.isEditingTitle,
)

internal fun detailContentUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailContentUiModel(
    relatedEntries = state.relatedEntries.map {
        RelatedEntryUiModel(it.id.value, it.title, it.type.toUiModel())
    },
    metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt),
    activities = state.history.map {
        DetailActivityUiModel(
            type = it.activityType.toUiModel(),
            source = it.source,
            createdAt = it.createdAt,
        )
    },
)

internal fun detailOtpUiModel(otp: OtpCodeState?): DetailOtpUiModel? = when (otp) {
    null -> null
    OtpCodeState.Pending -> DetailOtpUiModel.Pending
    is OtpCodeState.Ready -> DetailOtpUiModel.Ready(otp.code, otp.progress)
    is OtpCodeState.Failed -> DetailOtpUiModel.Failed
}

internal fun toDetailPresentationModel(
    state: DetailUiState,
    otp: OtpCodeState?,
    revisionReveals: Map<SensitiveFieldKey, SensitiveValue> = emptyMap(),
): DetailPresentationModel? {
    val entry = state.entry ?: return null
    val shared = detailContentUiModel(entry, state)
    val fields = DetailFieldProjection(state)
    val revealedUsername = state.revealed(FieldKey.USERNAME)
    val revealedPassword = state.revealed(FieldKey.PASSWORD)
    val password = fields.protected(FieldKey.PASSWORD, SensitiveFieldKey.PASSWORD)

    return DetailPresentationModel(
        header = detailHeaderUiModel(entry, state),
        content = DetailBodyUiModel(
            icon = DetailIconCardUiModel(
                iconName = entry.icon.name,
                iconCustomPath = entry.icon.customReference,
                iconColor = entry.icon.color,
                associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                entryType = entry.type.toUiModel(),
                title = entry.detailDisplayTitle,
                username = entry.username,
                associatedDomain = entry.associatedDomain,
            ),
            sections = state.sections.mapNotNullTo(linkedSetOf(), DetailSectionKey::toUiModel),
            credential = if (DetailSectionKey.CREDENTIAL in state.sections) {
                CredentialSectionUiState(
                    username = CredentialFieldUiState(
                        visible = entry.type != EntryType.WIFI && (
                            entry.username.isNotBlank() ||
                                SensitiveFieldKey.PASSWORD !in state.sensitiveFieldKeys
                            ),
                        revealedValue = revealedUsername?.asScopedSensitiveText(),
                        isEditing = state.fieldEdits.isEditing(FieldKey.USERNAME),
                        editedValue = state.fieldEdits.draft(FieldKey.USERNAME),
                        valueForEditing = revealedUsername.copyToString() ?: entry.username,
                    ),
                    password = CredentialFieldUiState(
                        visible = if (entry.type == EntryType.WIFI) {
                            !entry.secret.wifi?.password.isNullOrBlank()
                        } else {
                            password.present || entry.type != EntryType.LOGIN
                        },
                        revealedValue = revealedPassword?.asScopedSensitiveText(),
                        isEditing = state.fieldEdits.isEditing(FieldKey.PASSWORD),
                        editedValue = state.fieldEdits.draft(FieldKey.PASSWORD),
                        valueForEditing = revealedPassword.copyToString().orEmpty(),
                    ),
                )
            } else null,
            otp = detailOtpUiModel(otp),
            bankCard = if (DetailSectionKey.BANK_CARD in state.sections) {
                detailBankCardUiModel(entry, state, fields)
            } else null,
            identity = if (DetailSectionKey.IDENTITY in state.sections) {
                detailIdentityUiModel(entry, fields)
            } else null,
            ssh = if (DetailSectionKey.SSH in state.sections) {
                detailSshUiModel(entry, state, fields)
            } else null,
            seedPhrase = if (DetailSectionKey.SEED_PHRASE in state.sections) {
                detailSeedPhraseUiModel(fields)
            } else null,
            passkey = if (DetailSectionKey.PASSKEY in state.sections) {
                detailPasskeyUiModel(entry, fields)
            } else null,
            relatedEntries = shared.relatedEntries,
            tags = entry.tags,
            associations = DetailAssociatedInfoUiModel(
                domain = entry.associatedDomain,
                editedDomain = state.fieldEdits.draft(DetailDraftKey.Domain),
                isEditingDomain = state.fieldEdits.isEditing(DetailDraftKey.Domain),
            ),
            associatedApps = state.associatedApps.map(DetailInstalledApp::toPackagePickerItemUiModel),
            packagePickerApps = state.packagePickerApps.map(DetailInstalledApp::toPackagePickerItemUiModel),
            notes = DetailNotesUiModel(
                notes = entry.secret.notes,
                editedNotes = state.fieldEdits.draft(DetailDraftKey.Notes),
                isEditing = state.fieldEdits.isEditing(DetailDraftKey.Notes),
            ),
            metadata = shared.metadata,
            activities = shared.activities,
        ),
        overlays = DetailEditorOverlaysUiModel(
            tagEditor = state.tagEditor.toTagEditorUiModel(),
            faviconEditor = state.faviconEditor.toFaviconEditorUiModel(),
            savingTags = state.saveState == DetailSaveState.Saving(DetailEditCompletion.Tags),
            savingIcon = state.saveState == DetailSaveState.Saving(DetailEditCompletion.Icon),
            revisionHistory = state.revisions.toUiModel(revisionReveals),
        ),
    )
}

private fun DetailRevisionState.toUiModel(
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
                    before = difference.before?.toDisplayText(),
                    after = difference.after?.toDisplayText(),
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

internal fun DetailInstalledApp.toPackagePickerItemUiModel() = AppPackagePickerItemUiModel(
    label = label,
    packageName = packageName,
)

internal fun DetailTagEditorState.toTagEditorUiModel(): DetailTagEditorUiModel = when (this) {
    DetailTagEditorState.Closed -> DetailTagEditorUiModel.Hidden
    is DetailTagEditorState.Open -> DetailTagEditorUiModel.Visible(
        initialTags = initialTags,
        draftTags = draftTags,
        availableTags = availableTags,
        input = input,
        suggestions = suggestions,
        validationError = validationError?.toUiModel(),
        overlay = when (overlay) {
            DetailTagEditorOverlay.None -> TagEditorOverlayUiState.None
            DetailTagEditorOverlay.ConfirmDiscard -> TagEditorOverlayUiState.ConfirmDiscard
        },
    )
}

internal fun SensitiveValue?.asScopedSensitiveText(): ScopedSensitiveText {
    val source = this ?: return ScopedSensitiveText.Empty
    return object : ScopedSensitiveText {
        override val isEmpty: Boolean get() = source.isEmpty

        override fun <R> useChars(block: (CharArray) -> R): R {
            val chars = source.toCharArray()
            return try {
                block(chars)
            } finally {
                chars.fill('\u0000')
            }
        }

        override fun toString() = "***"
    }
}

private fun DetailSectionKey.toUiModel(): DetailSectionUiModel? = when (this) {
    DetailSectionKey.CREDENTIAL -> DetailSectionUiModel.CREDENTIAL
    DetailSectionKey.OTP -> DetailSectionUiModel.OTP
    DetailSectionKey.BANK_CARD -> DetailSectionUiModel.BANK_CARD
    DetailSectionKey.IDENTITY -> DetailSectionUiModel.IDENTITY
    DetailSectionKey.SSH -> DetailSectionUiModel.SSH
    DetailSectionKey.SEED_PHRASE -> DetailSectionUiModel.SEED_PHRASE
    DetailSectionKey.PASSKEY -> DetailSectionUiModel.PASSKEY
    DetailSectionKey.ENTRY_TYPE,
    DetailSectionKey.ASSOCIATED_INFO,
    DetailSectionKey.NOTES,
    DetailSectionKey.METADATA,
    DetailSectionKey.ACTIVITY -> null
}

private fun ActivityType.toUiModel(): DetailActivityTypeUiModel = when (this) {
    ActivityType.VIEW -> DetailActivityTypeUiModel.VIEW
    ActivityType.COPY_USERNAME -> DetailActivityTypeUiModel.COPY_USERNAME
    ActivityType.COPY_PASSWORD -> DetailActivityTypeUiModel.COPY_PASSWORD
    ActivityType.AUTOFILL -> DetailActivityTypeUiModel.AUTOFILL
    ActivityType.EXPORT -> DetailActivityTypeUiModel.EXPORT
    ActivityType.IMPORT -> DetailActivityTypeUiModel.IMPORT
    ActivityType.CREATE -> DetailActivityTypeUiModel.CREATE
    ActivityType.UPDATE -> DetailActivityTypeUiModel.UPDATE
    ActivityType.SENSITIVE_CHANGE -> DetailActivityTypeUiModel.SENSITIVE_CHANGE
    ActivityType.DELETE -> DetailActivityTypeUiModel.DELETE
    ActivityType.RESTORE -> DetailActivityTypeUiModel.RESTORE
}

private fun DetailTagValidationError.toUiModel(): TagEditorValidationErrorUiModel = when (this) {
    DetailTagValidationError.TOO_MANY_TAGS -> TagEditorValidationErrorUiModel.TOO_MANY_TAGS
    DetailTagValidationError.TAG_TOO_LONG -> TagEditorValidationErrorUiModel.TAG_TOO_LONG
}

private fun RevisionFieldValue.toDisplayText(): String = when (this) {
    is RevisionFieldValue.Text -> value
    is RevisionFieldValue.TextSet -> values.joinToString()
    is RevisionFieldValue.Flag -> value.toString()
    is RevisionFieldValue.EpochMillis -> value.toString()
    is RevisionFieldValue.Icon -> listOfNotNull(
        value.name,
        value.customReference,
        value.color,
    ).joinToString()
    is RevisionFieldValue.Credential -> value.toString()
    is RevisionFieldValue.CustomFields -> values.joinToString { "${it.name}: ${it.value}" }
    is RevisionFieldValue.Links -> values.joinToString { it.relationType.name }
    is RevisionFieldValue.AttachmentIds -> values.joinToString()
}

package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.policy.EntryTagValidationError
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.components.EntryClassificationInput
import com.aozijx.passly.presentation.shared.components.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.components.VaultIcons
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.mapItems
import com.aozijx.passly.feature.vault.presentation.detail.history.toRevisionSheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityHistoryUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedAppsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHeaderUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIconCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailMetadataUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailNotesUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailOtpUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPresentationModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedSectionsUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedEntryUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedPasswordUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.model.TagEditorValidationMessageUiModel
import com.aozijx.passly.feature.vault.presentation.navigation.VaultEditorDestinationRegistry

internal fun detailHeaderUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailHeaderUiModel(
    title = entry.detailDisplayTitle,
    favorite = entry.favorite,
    editedTitle = state.editedTitle,
    isEditingTitle = state.isEditingTitle,
    canEditFullForm = VaultEditorDestinationRegistry.supportsFullFormEdit(entry.type),
    entryType = entry.type,
)

internal fun detailOtpUiModel(otp: OtpCodeState?): DetailOtpUiModel? =
    when (otp) {
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
    val accountGroupEntries =
        state.accountGroupEntries.map {
            RelatedEntryUiModel(it.id.value, it.title, it.type)
        }
    val otherRelatedEntries =
        state.otherRelatedEntries.map {
            RelatedEntryUiModel(it.id.value, it.title, it.type)
        }
    val relatedPasswords =
        state.relatedPasswords.map {
            RelatedPasswordUiModel(
                linkId = it.linkId.value,
                targetEntryId = it.targetEntryId.value,
                title = it.title,
                username = it.username,
            )
        }
    val metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt)
    val activities =
        state.activityHistory.activities.map {
            DetailActivityUiModel(
                type = it.activityType,
                context = it.context,
                createdAt = it.createdAt,
            )
        }
    val fields = DetailFieldProjection(state)
    val revealedUsername = state.revealed(FieldKey.USERNAME)
    val revealedPassword = state.revealed(FieldKey.PASSWORD)
    val password = fields.protected(FieldKey.PASSWORD, SensitiveFieldKey.PASSWORD)

    return DetailPresentationModel(
        header = detailHeaderUiModel(entry, state),
        content =
            DetailBodyUiModel(
                icon =
                    DetailIconCardUiModel(
                        iconName = entry.icon.name,
                        iconCustomPath = entry.icon.customReference,
                        iconImageModel = toLocalIconImageModel(entry.icon.customReference),
                        iconColor = entry.icon.color,
                        associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                        entryType = entry.type,
                        title = entry.detailDisplayTitle,
                        associatedDomain = entry.associatedDomain,
                        visualCategory =
                            EntryVisualCategoryClassifier.classify(
                                EntryClassificationInput(
                                    entryType = entry.type,
                                    title = entry.detailDisplayTitle,
                                    username = entry.username,
                                    urls = setOfNotNull(entry.associatedDomain),
                                    domains = setOfNotNull(entry.associatedDomain),
                                    packageNames = entry.associations.applicationIds,
                                    appNames =
                                        setOfNotNull(
                                            entry.detailDisplayTitle.takeIf {
                                                entry.associations.applicationIds.isNotEmpty() && entry.associatedDomain == null
                                            },
                                        ),
                                ),
                            ),
                        iconDescriptionRes =
                            when {
                                !entry.icon.customReference.isNullOrBlank() -> R.string.vault_detail_favicon_custom_image
                                !entry.icon.name.isNullOrBlank() ->
                                    VaultIcons.findDefinition(entry.icon.name)?.labelRes
                                        ?: R.string.vault_detail_favicon_built_in
                                else -> R.string.vault_detail_favicon_default
                            },
                    ),
                sections = state.sections.toSet(),
                credential =
                    if (DetailSectionKey.CREDENTIAL in state.sections) {
                        CredentialSectionUiState(
                            username =
                                (
                                    entry.type != EntryType.WIFI && (
                                        entry.username.isNotBlank() ||
                                            SensitiveFieldKey.PASSWORD !in state.sensitiveFieldKeys
                                    )
                                ).takeIf { it }?.let {
                                    CredentialFieldUiState(
                                        titleRes = R.string.field_username,
                                        revealedValue = revealedUsername?.asScopedSensitiveText(),
                                        isEditing = state.fieldEdits.isEditing(FieldKey.USERNAME),
                                        editedValue = state.fieldEdits.draft(FieldKey.USERNAME),
                                        valueForEditing = revealedUsername.copyToString() ?: entry.username,
                                    )
                                },
                            password =
                                (
                                    if (entry.type == EntryType.WIFI) {
                                        !entry.secret.wifi?.password.isNullOrBlank()
                                    } else {
                                        password.isPresent || entry.type != EntryType.LOGIN
                                    }
                                ).takeIf { it }?.let {
                                    CredentialFieldUiState(
                                        titleRes = R.string.password_label,
                                        revealedValue = revealedPassword?.asScopedSensitiveText(),
                                        isEditing = state.fieldEdits.isEditing(FieldKey.PASSWORD),
                                        editedValue = state.fieldEdits.draft(FieldKey.PASSWORD),
                                        valueForEditing = revealedPassword.copyToString().orEmpty(),
                                    )
                                },
                        )
                    } else {
                        null
                    },
                otp = detailOtpUiModel(otp),
                canShowWifiQr = entry.type == EntryType.WIFI,
                bankCard =
                    if (DetailSectionKey.BANK_CARD in state.sections) {
                        detailBankCardUiModel(entry, state, fields)
                    } else {
                        null
                    },
                identity =
                    if (DetailSectionKey.IDENTITY in state.sections) {
                        detailIdentityUiModel(entry, fields)
                    } else {
                        null
                    },
                ssh =
                    if (DetailSectionKey.SSH in state.sections) {
                        detailSshUiModel(entry, state, fields)
                    } else {
                        null
                    },
                seedPhrase =
                    if (DetailSectionKey.SEED_PHRASE in state.sections) {
                        detailSeedPhraseUiModel(fields)
                    } else {
                        null
                    },
                passkey =
                    if (DetailSectionKey.PASSKEY in state.sections) {
                        detailPasskeyUiModel(entry, fields)
                    } else {
                        null
                    },
                relatedSections =
                    relatedSectionsUiModel(
                        accountGroupEntries = accountGroupEntries,
                        otherRelatedEntries = otherRelatedEntries,
                        relatedPasswords = relatedPasswords,
                        canBindPasswords = entry.type == EntryType.LOGIN && entry.deletedAt == null,
                    ),
                passwordBinding = state.passwordBinding,
                passwordUnlink = state.passwordUnlink,
                tags = entry.tags.toTagChipsUiState(),
                associations =
                    DetailAssociatedInfoUiModel(
                        domain = entry.associatedDomain,
                        editedDomain = state.fieldEdits.draft(DetailDraftKey.Domain),
                        isEditingDomain = state.fieldEdits.isEditing(DetailDraftKey.Domain),
                    ),
                associatedApps =
                    state.associatedApps
                        .map(DetailInstalledApp::toPackagePickerItemUiModel)
                        .takeIf { it.isNotEmpty() }
                        ?.let(DetailAssociatedAppsUiState::Items)
                        ?: DetailAssociatedAppsUiState.Empty,
                packagePickerApps = state.packagePickerApps.mapItems(DetailInstalledApp::toPackagePickerItemUiModel),
                notes =
                    DetailNotesUiModel(
                        notes = entry.secret.notes,
                        editedNotes = state.fieldEdits.draft(DetailDraftKey.Notes),
                        isEditing = state.fieldEdits.isEditing(DetailDraftKey.Notes),
                    ),
                metadata = metadata,
                activityFilter = state.activityFilter,
                activityHistory =
                    state.activityHistory.toUiState(
                        when (state.activityFilter) {
                        DetailActivityFilter.ALL -> activities
                        DetailActivityFilter.ACTIONS -> activities.filter { it.type.isMutation }
                        DetailActivityFilter.AUTOFILL -> activities.filter { it.type == ActivityType.AUTOFILL }
                        },
                    ),
                snapshotState = state.snapshotState,
            ),
        overlays =
            DetailEditorOverlaysUiModel(
                tagEditor = state.tagEditor.toTagEditorUiModel(),
                faviconEditor = state.faviconEditor.toFaviconEditorUiModel(),
                savingTags = state.saveState == DetailSaveState.Saving(DetailEditCompletion.Tags),
                savingIcon = state.saveState == DetailSaveState.Saving(DetailEditCompletion.Icon),
                revisionHistory = state.revisions.toRevisionSheetUiModel(revisionReveals),
            ),
    )
}

private fun relatedSectionsUiModel(
    accountGroupEntries: List<RelatedEntryUiModel>,
    otherRelatedEntries: List<RelatedEntryUiModel>,
    relatedPasswords: List<RelatedPasswordUiModel>,
    canBindPasswords: Boolean,
): DetailRelatedSectionsUiModel {
    val entries = accountGroupEntries + otherRelatedEntries
    return DetailRelatedSectionsUiModel(
        entries =
            entries
                .takeIf { it.isNotEmpty() }
                ?.let(DetailRelatedEntrySectionUiState::Items)
                ?: DetailRelatedEntrySectionUiState.Hidden,
        passwords =
            relatedPasswords
                .takeIf { it.isNotEmpty() }
                ?.let { DetailRelatedPasswordSectionUiState.Items(it, canBindPasswords) }
                ?: canBindPasswords
                    .takeIf { it }
                    ?.let { DetailRelatedPasswordSectionUiState.EmptyCanBind }
                ?: DetailRelatedPasswordSectionUiState.Hidden,
    )
}

private fun DetailActivityHistoryState.toUiState(
    activities: List<DetailActivityUiModel>,
): DetailActivityHistoryUiState =
    when (this) {
        is DetailActivityHistoryState.Loading -> DetailActivityHistoryUiState.Loading
        is DetailActivityHistoryState.Ready ->
            activities
                .takeIf { it.isNotEmpty() }
                ?.let(DetailActivityHistoryUiState::ReadyItems)
                ?: DetailActivityHistoryUiState.ReadyEmpty
        is DetailActivityHistoryState.Failed ->
            activities
                .takeIf { it.isNotEmpty() }
                ?.let(DetailActivityHistoryUiState::FailedItems)
                ?: DetailActivityHistoryUiState.FailedEmpty
    }

internal fun DetailInstalledApp.toPackagePickerItemUiModel() =
    AppPackagePickerItemUiModel(
        label = label,
        packageName = packageName,
    )

internal fun DetailTagEditorState.toTagEditorUiModel(): DetailTagEditorUiModel =
    when (this) {
        DetailTagEditorState.Closed -> {
            DetailTagEditorUiModel.Hidden
        }

        is DetailTagEditorState.Open -> {
            DetailTagEditorUiModel.Visible(
                draftTags = draftTags.toTagChipsUiState(),
                availableTags = availableTags,
                input = input,
                suggestions = suggestions.toTagChipsUiState(),
                validationMessage = validationError?.toMessageUiModel(),
                showDiscardConfirmation = showDiscardConfirmation,
                dirty = dirty,
            )
        }
    }

private fun Set<String>.toTagChipsUiState(): DetailTagChipsUiState =
    toList()
        .takeIf { it.isNotEmpty() }
        ?.let(DetailTagChipsUiState::Items)
        ?: DetailTagChipsUiState.Empty

private fun List<String>.toTagChipsUiState(): DetailTagChipsUiState =
    takeIf { it.isNotEmpty() }
        ?.let(DetailTagChipsUiState::Items)
        ?: DetailTagChipsUiState.Empty

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

private fun EntryTagValidationError.toMessageUiModel(): TagEditorValidationMessageUiModel =
    when (this) {
        is EntryTagValidationError.TooMany ->
            TagEditorValidationMessageUiModel(
                pluralResourceId = R.plurals.vault_detail_tag_too_many,
                quantity = maximum,
            )

        is EntryTagValidationError.TooLong ->
            TagEditorValidationMessageUiModel(
                pluralResourceId = R.plurals.vault_detail_tag_too_long,
                quantity = maximum,
            )
    }

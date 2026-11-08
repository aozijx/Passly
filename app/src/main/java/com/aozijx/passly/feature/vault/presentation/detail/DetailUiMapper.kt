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
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityHistoryUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedAppsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodySectionUiModel
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
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.TagEditorValidationUiState
import com.aozijx.passly.feature.vault.presentation.navigation.VaultEditorDestinationRegistry
import com.aozijx.passly.feature.vault.presentation.detail.DetailField

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

internal fun detailOtpUiModel(otp: OtpCodeState?): DetailOtpUiModel =
    when (otp) {
        null -> DetailOtpUiModel.NotRequested
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
                sections = detailBodySectionsUiModels(entry, state, otp, fields, password),
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
                savingTags =
                    state.saveState is DetailSaveState.Saving &&
                        state.saveState.completion == DetailEditCompletion.Tags,
                savingIcon =
                    state.saveState is DetailSaveState.Saving &&
                        state.saveState.completion == DetailEditCompletion.Icon,
                revisionHistory = state.revisions.toRevisionSheetUiModel(revisionReveals),
            ),
    )
}

private fun detailBodySectionsUiModels(
    entry: Entry,
    state: DetailUiState,
    otp: OtpCodeState?,
    fields: DetailFieldProjection,
    password: ProtectedDetailFieldUiState,
): List<DetailBodySectionUiModel> =
    buildList {
        if (DetailSectionKey.CREDENTIAL in state.sections) {
            val credentialFields =
                buildList {
                    if (
                        entry.type != EntryType.WIFI &&
                        (entry.username.isNotBlank() || SensitiveFieldKey.PASSWORD !in state.sensitiveFieldKeys)
                    ) {
                        add(
                            CredentialFieldUiState(
                                field = DetailField.USERNAME,
                                titleRes = R.string.field_username,
                                revealedValue = state.revealed(FieldKey.USERNAME)?.asScopedSensitiveText(),
                                isEditing = state.fieldEdits.isEditing(FieldKey.USERNAME),
                                editedValue = state.fieldEdits.draft(FieldKey.USERNAME),
                                valueForEditing = state.revealed(FieldKey.USERNAME).copyToString() ?: entry.username,
                            ),
                        )
                    }
                    val shouldShowPassword =
                        if (entry.type == EntryType.WIFI) {
                            !entry.secret.wifi?.password.isNullOrBlank()
                        } else {
                            password.isPresent || entry.type != EntryType.LOGIN
                        }
                    if (shouldShowPassword) {
                        add(
                            CredentialFieldUiState(
                                field = DetailField.PASSWORD,
                                titleRes = R.string.password_label,
                                revealedValue = state.revealed(FieldKey.PASSWORD)?.asScopedSensitiveText(),
                                isEditing = state.fieldEdits.isEditing(FieldKey.PASSWORD),
                                editedValue = state.fieldEdits.draft(FieldKey.PASSWORD),
                                valueForEditing = state.revealed(FieldKey.PASSWORD).copyToString().orEmpty(),
                            ),
                        )
                    }
                }
            if (credentialFields.isNotEmpty()) {
                add(DetailBodySectionUiModel.Credential(credentialFields))
            }
        }
        if (entry.type == EntryType.WIFI) add(DetailBodySectionUiModel.WifiQr)
        if (DetailSectionKey.OTP in state.sections) add(DetailBodySectionUiModel.Otp(detailOtpUiModel(otp)))
        if (DetailSectionKey.BANK_CARD in state.sections) {
            add(DetailBodySectionUiModel.BankCard(detailBankCardUiModel(entry, state, fields)))
        }
        if (DetailSectionKey.IDENTITY in state.sections) {
            add(DetailBodySectionUiModel.Identity(detailIdentityUiModel(entry, fields)))
        }
        if (DetailSectionKey.SSH in state.sections) {
            add(DetailBodySectionUiModel.Ssh(detailSshUiModel(entry, state, fields)))
        }
        if (DetailSectionKey.SEED_PHRASE in state.sections) {
            add(DetailBodySectionUiModel.SeedPhrase(detailSeedPhraseUiModel(fields)))
        }
        if (DetailSectionKey.PASSKEY in state.sections) {
            add(DetailBodySectionUiModel.Passkey(detailPasskeyUiModel(entry, fields)))
        }
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
                validation = validation.toTagEditorValidationUiState(),
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

private fun DetailTagValidationState.toTagEditorValidationUiState(): TagEditorValidationUiState =
    when (this) {
        DetailTagValidationState.None -> TagEditorValidationUiState.None
        is DetailTagValidationState.Invalid -> error.toUiState()
    }

private fun EntryTagValidationError.toUiState(): TagEditorValidationUiState.Invalid =
    when (this) {
        is EntryTagValidationError.TooMany ->
            TagEditorValidationUiState.Invalid(
                pluralResourceId = R.plurals.vault_detail_tag_too_many,
                quantity = maximum,
            )

        is EntryTagValidationError.TooLong ->
            TagEditorValidationUiState.Invalid(
                pluralResourceId = R.plurals.vault_detail_tag_too_long,
                quantity = maximum,
            )
    }

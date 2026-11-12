package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.titleEditValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.mapItems
import com.aozijx.passly.feature.vault.presentation.detail.history.toHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedAppsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainValueUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodySectionUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHeaderUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIconCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailMetadataUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailNotesUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPageUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPresentationModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedSectionsUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTitleUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedEntryUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedPasswordRowUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedPasswordUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ScopedSensitiveText
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.editor.VaultEditorCapabilities
import com.aozijx.passly.presentation.shared.components.VaultIcons
import com.aozijx.passly.presentation.shared.entry.labelRes
import com.aozijx.passly.presentation.shared.entryvisual.EntryClassificationInput
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel

internal fun detailHeaderUiModel(
    entry: Entry,
    state: DetailUiState,
) = DetailHeaderUiModel(
    title =
        when (val edit = state.fieldEdits.edit(DetailDraftKey.Title)) {
            DetailFieldEdit.NotEditing -> DetailTitleUiState.Display(entry.titleEditValue)
            is DetailFieldEdit.Editing -> DetailTitleUiState.Editing(edit.draft)
        },
    favorite = entry.favorite,
    canEditFullForm = VaultEditorCapabilities.supportsFullFormEdit(entry.type),
)

internal fun toDetailPageUiState(
    state: DetailUiState,
    revisionReveals: Map<SensitiveFieldKey, SensitiveValue> = emptyMap(),
): DetailPageUiState =
    when (val snapshot = state.snapshotState) {
        DetailSnapshotState.Loading -> {
            DetailPageUiState.Loading
        }

        DetailSnapshotState.NotFound -> {
            DetailPageUiState.NotFound
        }

        DetailSnapshotState.Failed -> {
            DetailPageUiState.Failed
        }

        is DetailSnapshotState.Content -> {
            DetailPageUiState.Content(
                toDetailPresentationModel(
                    state,
                    snapshot.entry,
                    snapshot.refreshPhase,
                    revisionReveals,
                ),
            )
        }
    }

internal fun toDetailPresentationModel(
    state: DetailUiState,
    entry: Entry,
    snapshotRefreshPhase: DetailSnapshotRefreshPhase,
    revisionReveals: Map<SensitiveFieldKey, SensitiveValue> = emptyMap(),
): DetailPresentationModel {
    val accountGroupEntries =
        state.accountGroupEntries.map {
            RelatedEntryUiModel(it.id.value, it.title, it.type.labelRes)
        }
    val otherRelatedEntries =
        state.otherRelatedEntries.map {
            RelatedEntryUiModel(it.id.value, it.title, it.type.labelRes)
        }
    val metadata = DetailMetadataUiModel(entry.createdAt, entry.updatedAt)
    val activities =
        when (val activityHistory = state.activityHistory) {
            DetailActivityHistoryState.Loading,
            DetailActivityHistoryState.Failed,
            -> {
                emptyList()
            }

            is DetailActivityHistoryState.Content -> {
                activityHistory.activities.map(EntryActivity::toDetailHistoryItemUiModel)
            }
        }
    val fields = DetailFieldProjection(state)
    val hasPassword = SensitiveFieldKey.PASSWORD in state.sensitiveFieldKeys
    val visualCategory =
        EntryVisualCategoryClassifier.classify(
            EntryClassificationInput(
                entryType = entry.type,
                title = entry.titleEditValue,
                username = entry.username,
                urls = setOfNotNull(entry.associatedDomain),
                domains = setOfNotNull(entry.associatedDomain),
                packageNames = entry.associations.applicationIds,
                appNames =
                    setOfNotNull(
                        entry.titleEditValue.takeIf {
                            entry.associations.applicationIds.isNotEmpty() && entry.associatedDomain == null
                        },
                    ),
            ),
        )

    return DetailPresentationModel(
        entryId = entry.id.value,
        header = detailHeaderUiModel(entry, state),
        content =
            DetailBodyUiModel(
                icon =
                    DetailIconCardUiModel(
                        icon =
                            vaultItemIconUiModel(
                                iconName = entry.icon.name,
                                customImagePath = entry.icon.customReference,
                                iconColor = entry.icon.color,
                                associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                                visualCategory = visualCategory,
                            ),
                        iconDescriptionRes =
                            when {
                                !entry.icon.customReference.isNullOrBlank() -> {
                                    R.string.vault_detail_favicon_custom_image
                                }

                                !entry.icon.name.isNullOrBlank() -> {
                                    VaultIcons.findDefinition(entry.icon.name)?.labelRes
                                        ?: R.string.vault_detail_favicon_built_in
                                }

                                else -> {
                                    R.string.vault_detail_favicon_default
                                }
                            },
                    ),
                sections = detailBodySectionsUiModels(entry, state, fields, hasPassword),
                relatedSections =
                    relatedSectionsUiModel(
                        accountGroupEntries = accountGroupEntries,
                        otherRelatedEntries = otherRelatedEntries,
                        relatedPasswords = state.relatedPasswords,
                        passwordUnlink = state.passwordUnlink,
                        canBindPasswords = entry.type == EntryType.LOGIN && entry.deletedAt == null,
                    ),
                passwordBinding = state.passwordBinding,
                passwordUnlink = state.passwordUnlink,
                tags = entry.tags.toDetailTagChipsUiState(),
                associations =
                    DetailAssociatedInfoUiModel(
                        domain =
                            when (val edit = state.fieldEdits.edit(DetailDraftKey.Domain)) {
                                is DetailFieldEdit.Editing -> {
                                    DetailAssociatedDomainUiState.Editing(edit.draft)
                                }

                                DetailFieldEdit.NotEditing -> {
                                    DetailAssociatedDomainUiState.Display(
                                        value =
                                            entry.associatedDomain
                                                ?.let(DetailAssociatedDomainValueUiState::Value)
                                                ?: DetailAssociatedDomainValueUiState.NotSet,
                                    )
                                }
                            },
                    ),
                associatedApps =
                    state.associatedApps
                        .map(DetailInstalledApp::toPackagePickerItemUiModel)
                        .takeIf { it.isNotEmpty() }
                        ?.let(DetailAssociatedAppsUiState::Items)
                        ?: DetailAssociatedAppsUiState.Empty,
                packagePickerApps = state.packagePickerApps.mapItems(DetailInstalledApp::toPackagePickerItemUiModel),
                notes =
                    when (val edit = state.fieldEdits.edit(DetailDraftKey.Notes)) {
                        is DetailFieldEdit.Editing -> {
                            DetailNotesUiState.Editing(edit.draft)
                        }

                        DetailFieldEdit.NotEditing -> {
                            entry.secret.notes
                                ?.let(DetailNotesUiState::Markdown)
                                ?: DetailNotesUiState.NotSet
                        }
                    },
                metadata = metadata,
                snapshotRefreshPhase = snapshotRefreshPhase,
            ),
        overlays =
            DetailEditorOverlaysUiModel(
                tagEditor = state.tagEditor,
                faviconEditor = state.faviconEditor,
                savingTags =
                    state.saveState is DetailSaveState.Saving &&
                        state.saveState.completion == DetailEditCompletion.Tags,
                savingIcon =
                    state.saveState is DetailSaveState.Saving &&
                        state.saveState.completion == DetailEditCompletion.Icon,
                history =
                    state.revisions.toHistorySheetUiModel(
                        filter = state.historyFilter,
                        activityHistory = state.activityHistory,
                        activityItems = activities,
                        reveals = revisionReveals,
                    ),
            ),
    )
}

private fun detailBodySectionsUiModels(
    entry: Entry,
    state: DetailUiState,
    fields: DetailFieldProjection,
    hasPassword: Boolean,
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
                            credentialFieldUiState(
                                field = DetailField.USERNAME,
                                titleRes = R.string.field_username,
                                fieldKey = FieldKey.USERNAME,
                                state = state,
                            ),
                        )
                    }
                    val shouldShowPassword =
                        if (entry.type == EntryType.WIFI) {
                            !entry.secret.wifi
                                ?.password
                                .isNullOrBlank()
                        } else {
                            hasPassword || entry.type != EntryType.LOGIN
                        }
                    if (shouldShowPassword) {
                        add(
                            credentialFieldUiState(
                                field = DetailField.PASSWORD,
                                titleRes = R.string.password_label,
                                fieldKey = FieldKey.PASSWORD,
                                state = state,
                            ),
                        )
                    }
                }
            if (credentialFields.isNotEmpty()) {
                add(DetailBodySectionUiModel.Credential(credentialFields))
            }
        }
        if (entry.type == EntryType.WIFI) add(DetailBodySectionUiModel.WifiQr)
        if (DetailSectionKey.OTP in state.sections) {
            val otpType = checkNotNull(entry.secret.otp).config.type
            add(DetailBodySectionUiModel.Otp(otpType))
        }
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

private fun credentialFieldUiState(
    field: DetailField,
    @StringRes titleRes: Int,
    fieldKey: FieldKey,
    state: DetailUiState,
): CredentialFieldUiState {
    val editKey = DetailDraftKey.EntryField(fieldKey)
    val revealed = state.revealed(fieldKey)
    return when (val edit = state.fieldEdits.edit(editKey)) {
        is DetailFieldEdit.Editing -> {
            CredentialFieldUiState.Editing(
                field = field,
                titleRes = titleRes,
                draft = edit.draft,
                showModifiedHint = revealed.differsFrom(edit.draft),
            )
        }

        DetailFieldEdit.NotEditing -> {
            revealed
                ?.let { CredentialFieldUiState.Revealed(field, titleRes, it.asScopedSensitiveText()) }
                ?: CredentialFieldUiState.Hidden(field, titleRes)
        }
    }
}

private fun SensitiveValue?.differsFrom(value: String): Boolean {
    val original = this ?: return false
    val originalChars = original.toCharArray()
    val candidateChars = value.toCharArray()
    return try {
        !originalChars.contentEquals(candidateChars)
    } finally {
        originalChars.fill('\u0000')
        candidateChars.fill('\u0000')
    }
}

private fun relatedSectionsUiModel(
    accountGroupEntries: List<RelatedEntryUiModel>,
    otherRelatedEntries: List<RelatedEntryUiModel>,
    relatedPasswords: List<RelatedPasswordEntry>,
    passwordUnlink: RelatedPasswordUnlinkUiState,
    canBindPasswords: Boolean,
): DetailRelatedSectionsUiModel {
    val entries = accountGroupEntries + otherRelatedEntries
    val passwordRows =
        relatedPasswords.map { related ->
            val isRemoving =
                when (passwordUnlink) {
                    RelatedPasswordUnlinkUiState.Closed -> {
                        false
                    }

                    is RelatedPasswordUnlinkUiState.Open -> {
                        passwordUnlink.phase == RelatedPasswordUnlinkPhase.Removing &&
                            passwordUnlink.target.targetEntryId == related.targetEntryId.value
                    }
                }
            RelatedPasswordUiModel(
                linkId = related.linkId.value,
                targetEntryId = related.targetEntryId.value,
                title = related.title,
                username = related.username,
                unlinkActionState =
                    if (isRemoving) {
                        RelatedPasswordRowUnlinkUiState.Removing
                    } else {
                        RelatedPasswordRowUnlinkUiState.Available
                    },
            )
        }
    return DetailRelatedSectionsUiModel(
        entries =
            entries
                .takeIf { it.isNotEmpty() }
                ?.let(DetailRelatedEntrySectionUiState::Items)
                ?: DetailRelatedEntrySectionUiState.Hidden,
        passwords =
            passwordRows
                .takeIf { it.isNotEmpty() }
                ?.let { DetailRelatedPasswordSectionUiState.Items(it, canBindPasswords) }
                ?: canBindPasswords
                    .takeIf { it }
                    ?.let { DetailRelatedPasswordSectionUiState.EmptyCanBind }
                ?: DetailRelatedPasswordSectionUiState.Hidden,
    )
}

internal fun DetailInstalledApp.toPackagePickerItemUiModel() =
    AppPackagePickerItemUiModel(
        label = label,
        packageName = packageName,
    )

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

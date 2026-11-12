package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconEditorState
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailSnapshotRefreshPhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagEditorState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkUiState

data class DetailPresentationModel(
    val entryId: String,
    val header: DetailHeaderUiModel,
    val content: DetailBodyUiModel,
    val overlays: DetailEditorOverlaysUiModel,
)

sealed interface DetailPageUiState {
    data object Loading : DetailPageUiState

    data object NotFound : DetailPageUiState

    data object Failed : DetailPageUiState

    data class Content(
        val presentation: DetailPresentationModel,
    ) : DetailPageUiState
}

data class DetailBodyUiModel(
    val icon: DetailIconCardUiModel,
    val sections: List<DetailBodySectionUiModel>,
    val relatedSections: DetailRelatedSectionsUiModel,
    val passwordBinding: RelatedPasswordBindingUiState,
    val passwordUnlink: RelatedPasswordUnlinkUiState,
    val tags: DetailTagChipsUiState,
    val associations: DetailAssociatedInfoUiModel,
    val associatedApps: DetailAssociatedAppsUiState,
    val packagePickerApps: AppPackagePickerUiState<AppPackagePickerItemUiModel>,
    val notes: DetailNotesUiState,
    val metadata: DetailMetadataUiModel,
    val snapshotRefreshPhase: DetailSnapshotRefreshPhase,
)

sealed interface DetailBodySectionUiModel {
    data class Credential(
        val fields: List<CredentialFieldUiState>,
    ) : DetailBodySectionUiModel {
        init {
            require(fields.isNotEmpty())
        }
    }

    data object WifiQr : DetailBodySectionUiModel

    data class Otp(
        val type: OtpType,
    ) : DetailBodySectionUiModel

    data class BankCard(
        val state: DetailBankCardUiModel,
    ) : DetailBodySectionUiModel

    data class Identity(
        val state: DetailIdentityUiModel,
    ) : DetailBodySectionUiModel

    data class Ssh(
        val state: DetailSshUiModel,
    ) : DetailBodySectionUiModel

    data class SeedPhrase(
        val state: DetailSeedPhraseUiModel,
    ) : DetailBodySectionUiModel

    data class Passkey(
        val state: DetailPasskeyUiModel,
    ) : DetailBodySectionUiModel
}

data class DetailRelatedSectionsUiModel(
    val entries: DetailRelatedEntrySectionUiState,
    val passwords: DetailRelatedPasswordSectionUiState,
)

sealed interface DetailRelatedEntrySectionUiState {
    data object Hidden : DetailRelatedEntrySectionUiState

    data class Items(
        val entries: List<RelatedEntryUiModel>,
    ) : DetailRelatedEntrySectionUiState {
        init {
            require(entries.isNotEmpty())
        }
    }
}

sealed interface DetailRelatedPasswordSectionUiState {
    data object Hidden : DetailRelatedPasswordSectionUiState

    data object EmptyCanBind : DetailRelatedPasswordSectionUiState

    data class Items(
        val entries: List<RelatedPasswordUiModel>,
        val canBind: Boolean,
    ) : DetailRelatedPasswordSectionUiState {
        init {
            require(entries.isNotEmpty())
        }
    }
}

sealed interface DetailAssociatedAppsUiState {
    data object Empty : DetailAssociatedAppsUiState

    data class Items(
        val apps: List<AppPackagePickerItemUiModel>,
    ) : DetailAssociatedAppsUiState {
        init {
            require(apps.isNotEmpty())
        }
    }
}

sealed interface DetailSeedPhraseUiModel {
    val isRevealed: Boolean
        get() = this is RevealedWithoutWords || this is RevealedWords

    val canReveal: Boolean
        get() = this is Hidden

    data object Absent : DetailSeedPhraseUiModel

    data object Hidden : DetailSeedPhraseUiModel

    data object RevealedWithoutWords : DetailSeedPhraseUiModel

    data class RevealedWords(
        val words: List<String>,
    ) : DetailSeedPhraseUiModel {
        init {
            require(words.isNotEmpty())
        }
    }
}

data class DetailPasskeyUiModel(
    val data: DetailPasskeyDataUiState,
    val hardwareKeyInfo: DetailHardwareKeyInfoUiState,
)

data class DetailEditorOverlaysUiModel(
    val tagEditor: DetailTagEditorState,
    val faviconEditor: DetailFaviconEditorState,
    val savingTags: Boolean,
    val savingIcon: Boolean,
    val history: DetailHistorySheetUiModel,
)

data class DetailRevisionItemUiModel(
    val id: String,
    val version: Int,
    val storedVersion: Int,
    val createdAtMs: Long,
    val change: RevisionChange,
    val changedFields: List<DetailRevisionFieldLabelUiState>,
)

data class DetailRevisionDifferenceUiModel(
    val field: RevisionFieldId,
    val label: DetailRevisionFieldLabelUiState,
    @get:StringRes val changeLabelRes: Int,
    val content: DetailRevisionDifferenceContentUiState,
    val revealAction: DetailRevisionRevealActionUiState,
)

sealed interface DetailRevisionFieldLabelUiState {
    data class Resource(
        @get:StringRes @param:StringRes val resource: Int,
    ) : DetailRevisionFieldLabelUiState

    data class Named(
        @get:StringRes @param:StringRes val resource: Int,
        val name: String,
    ) : DetailRevisionFieldLabelUiState
}

sealed interface DetailRevisionDifferenceContentUiState {
    data class Values(
        val before: DetailRevisionValueUiModel,
        val after: DetailRevisionValueUiModel,
    ) : DetailRevisionDifferenceContentUiState

    data object ProtectedHidden : DetailRevisionDifferenceContentUiState

    data class ProtectedRevealed(
        val value: ScopedSensitiveText,
    ) : DetailRevisionDifferenceContentUiState
}

sealed interface DetailRevisionRevealActionUiState {
    data object Unavailable : DetailRevisionRevealActionUiState

    data class Available(
        val fieldKey: SensitiveFieldKey,
    ) : DetailRevisionRevealActionUiState
}

sealed interface DetailRevisionValueUiModel {
    data object Absent : DetailRevisionValueUiModel

    data class Text(
        val value: String,
    ) : DetailRevisionValueUiModel

    data class Flag(
        val value: Boolean,
    ) : DetailRevisionValueUiModel

    data class EpochMillis(
        val value: Long,
    ) : DetailRevisionValueUiModel

    enum class Icon : DetailRevisionValueUiModel { DEFAULT, CUSTOM }

    data class Links(
        val count: Int,
    ) : DetailRevisionValueUiModel {
        init {
            require(count >= 0)
        }
    }

    data class Attachments(
        val count: Int,
    ) : DetailRevisionValueUiModel {
        init {
            require(count >= 0)
        }
    }
}

sealed interface DetailHistorySheetUiModel {
    data object Hidden : DetailHistorySheetUiModel

    data class ListPage(
        val content: DetailHistoryListUiState,
    ) : DetailHistorySheetUiModel

    data class ComparisonPage(
        val revisionId: String,
        val content: DetailRevisionComparisonUiState,
    ) : DetailHistorySheetUiModel
}

data class DetailHistoryListUiState(
    val filter: DetailHistoryFilter,
    val items: List<DetailHistoryItemUiModel>,
    val activitySource: DetailHistoryLoadState,
    val revisionSource: DetailHistoryLoadState,
) {
    val isLoading: Boolean
        get() = activitySource == DetailHistoryLoadState.LOADING || revisionSource == DetailHistoryLoadState.LOADING

    val isEmptyResult: Boolean
        get() =
            items.isEmpty() && activitySource == DetailHistoryLoadState.READY &&
                revisionSource == DetailHistoryLoadState.READY
}

enum class DetailHistoryLoadState { LOADING, READY, FAILED }

sealed interface DetailRevisionComparisonUiState {
    data object Loading : DetailRevisionComparisonUiState

    data class Ready(
        val content: DetailRevisionComparisonContentUiState,
        val restore: DetailRevisionRestorePhase,
    ) : DetailRevisionComparisonUiState

    data object Failed : DetailRevisionComparisonUiState
}

sealed interface DetailRevisionComparisonContentUiState {
    data object NoChanges : DetailRevisionComparisonContentUiState

    data class Changes(
        val differences: List<DetailRevisionDifferenceUiModel>,
    ) : DetailRevisionComparisonContentUiState {
        init {
            require(differences.isNotEmpty())
        }
    }
}

val DetailRevisionComparisonUiState.Ready.restoreEnabled: Boolean
    get() = restore == DetailRevisionRestorePhase.Idle && content is DetailRevisionComparisonContentUiState.Changes

val DetailRevisionComparisonUiState.restoreInProgress: Boolean
    get() =
        when (this) {
            DetailRevisionComparisonUiState.Loading,
            DetailRevisionComparisonUiState.Failed,
            -> false

            is DetailRevisionComparisonUiState.Ready -> restore == DetailRevisionRestorePhase.Restoring
        }

val DetailHistorySheetUiModel.blocksDismissal: Boolean
    get() =
        when (this) {
            DetailHistorySheetUiModel.Hidden,
            is DetailHistorySheetUiModel.ListPage,
            -> false

            is DetailHistorySheetUiModel.ComparisonPage -> content.restoreInProgress
        }

val DetailHistorySheetUiModel.restoreConfirmationVisible: Boolean
    get() =
        when (this) {
            DetailHistorySheetUiModel.Hidden,
            is DetailHistorySheetUiModel.ListPage,
            -> {
                false
            }

            is DetailHistorySheetUiModel.ComparisonPage -> {
                when (val comparison = content) {
                    DetailRevisionComparisonUiState.Loading,
                    DetailRevisionComparisonUiState.Failed,
                    -> false

                    is DetailRevisionComparisonUiState.Ready -> comparison.restore == DetailRevisionRestorePhase.Confirming
                }
            }
        }

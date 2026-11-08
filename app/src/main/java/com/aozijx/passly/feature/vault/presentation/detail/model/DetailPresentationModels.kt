package com.aozijx.passly.feature.vault.presentation.detail.model

import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityFilter
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.DetailSnapshotState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingUiState

data class DetailPresentationModel(
    val header: DetailHeaderUiModel,
    val content: DetailBodyUiModel,
    val overlays: DetailEditorOverlaysUiModel,
)

data class DetailBodyUiModel(
    val icon: DetailIconCardUiModel,
    val sections: Set<DetailSectionKey>,
    val credential: CredentialSectionUiState?,
    val otp: DetailOtpUiModel,
    val canShowWifiQr: Boolean,
    val bankCard: DetailBankCardUiModel?,
    val identity: DetailIdentityUiModel?,
    val ssh: DetailSshUiModel?,
    val seedPhrase: DetailSeedPhraseUiModel?,
    val passkey: DetailPasskeyUiModel?,
    val relatedSections: DetailRelatedSectionsUiModel,
    val passwordBinding: RelatedPasswordBindingUiState,
    val passwordUnlink: RelatedPasswordUnlinkUiState,
    val tags: DetailTagChipsUiState,
    val associations: DetailAssociatedInfoUiModel,
    val associatedApps: DetailAssociatedAppsUiState,
    val packagePickerApps: AppPackagePickerUiState<AppPackagePickerItemUiModel>,
    val notes: DetailNotesUiModel,
    val metadata: DetailMetadataUiModel,
    val activityFilter: DetailActivityFilter,
    val activityHistory: DetailActivityHistoryUiState,
    val snapshotState: DetailSnapshotState,
)

sealed interface DetailActivityHistoryUiState {
    data object Loading : DetailActivityHistoryUiState

    data object ReadyEmpty : DetailActivityHistoryUiState

    data class ReadyItems(val activities: List<DetailActivityUiModel>) : DetailActivityHistoryUiState {
        init {
            require(activities.isNotEmpty())
        }
    }

    data object FailedEmpty : DetailActivityHistoryUiState

    data class FailedItems(val activities: List<DetailActivityUiModel>) : DetailActivityHistoryUiState {
        init {
            require(activities.isNotEmpty())
        }
    }
}

data class DetailRelatedSectionsUiModel(
    val entries: DetailRelatedEntrySectionUiState,
    val passwords: DetailRelatedPasswordSectionUiState,
)

sealed interface DetailRelatedEntrySectionUiState {
    data object Hidden : DetailRelatedEntrySectionUiState

    data class Items(val entries: List<RelatedEntryUiModel>) : DetailRelatedEntrySectionUiState {
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

    data class Items(val apps: List<AppPackagePickerItemUiModel>) : DetailAssociatedAppsUiState {
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

    data class RevealedWords(val words: List<String>) : DetailSeedPhraseUiModel {
        init {
            require(words.isNotEmpty())
        }
    }
}

data class DetailPasskeyUiModel(
    val data: ProtectedDetailFieldUiState,
    val hardwareKeyInfo: String?,
)

data class DetailEditorOverlaysUiModel(
    val tagEditor: DetailTagEditorUiModel,
    val faviconEditor: DetailFaviconEditorUiModel,
    val savingTags: Boolean,
    val savingIcon: Boolean,
    val revisionHistory: DetailRevisionSheetUiModel,
)

data class DetailRevisionItemUiModel(
    val id: String,
    val version: Int,
    val createdAtMs: Long,
    val change: RevisionChange,
)

data class DetailRevisionDifferenceUiModel(
    val field: RevisionFieldId,
    val kind: RevisionDifferenceKind,
    val before: DetailRevisionValueUiModel,
    val after: DetailRevisionValueUiModel,
    val revealedValue: ScopedSensitiveText? = null,
)

sealed interface DetailRevisionValueUiModel {
    data object Absent : DetailRevisionValueUiModel

    data class Text(val value: String) : DetailRevisionValueUiModel

    data class Flag(val value: Boolean) : DetailRevisionValueUiModel

    data class EpochMillis(val value: Long) : DetailRevisionValueUiModel

    enum class Icon : DetailRevisionValueUiModel { DEFAULT, CUSTOM }

    data class Links(val count: Int) : DetailRevisionValueUiModel {
        init {
            require(count >= 0)
        }
    }

    data class Attachments(val count: Int) : DetailRevisionValueUiModel {
        init {
            require(count >= 0)
        }
    }
}

sealed interface DetailRevisionSheetUiModel {
    data object Hidden : DetailRevisionSheetUiModel

    data class ListPage(
        val content: DetailRevisionListUiState,
    ) : DetailRevisionSheetUiModel

    data class ComparisonPage(
        val revisionId: String,
        val content: DetailRevisionComparisonUiState,
    ) : DetailRevisionSheetUiModel
}

sealed interface DetailRevisionListUiState {
    data object Loading : DetailRevisionListUiState

    data object ReadyEmpty : DetailRevisionListUiState

    data class ReadyItems(
        val revisions: List<DetailRevisionItemUiModel>,
    ) : DetailRevisionListUiState {
        init {
            require(revisions.isNotEmpty())
        }
    }

    data object Failed : DetailRevisionListUiState
}

sealed interface DetailRevisionComparisonUiState {
    data object Loading : DetailRevisionComparisonUiState

    data class Ready(
        val differences: List<DetailRevisionDifferenceUiModel>,
        val restore: DetailRevisionRestorePhase,
    ) : DetailRevisionComparisonUiState

    data object Failed : DetailRevisionComparisonUiState
}

package com.aozijx.passly.feature.detail.contract

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.sensitive.SensitiveValue

sealed interface DetailUiAction {
    data class Initialize(val initialEntry: Entry) : DetailUiAction
    data class SyncEntry(val entry: Entry) : DetailUiAction
    data class CommitEntryUpdate(val entry: Entry) : DetailUiAction

    object StartTitleEdit : DetailUiAction
    object CancelTitleEdit : DetailUiAction
    data class UpdateEditedTitle(val value: String) : DetailUiAction

    object SaveTitle : DetailUiAction
    object ToggleFavorite : DetailUiAction

    data class RevealField(val key: String, val value: SensitiveValue?) : DetailUiAction
    data class ToggleVisibility(val key: String) : DetailUiAction
    data class SaveField(val key: String, val newValue: String) : DetailUiAction
    data class RevealHighSensitivityField(val key: String) : DetailUiAction
    data class RevealHighSensitivityFields(val keys: Set<String>) : DetailUiAction
    data class DownloadFavicon(val domain: String) : DetailUiAction

    data class RecordAction(val field: String, val type: ActivityType) : DetailUiAction
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailUiAction
    object ClearSensitiveState : DetailUiAction
}

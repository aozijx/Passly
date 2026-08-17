package com.aozijx.passly.feature.detail.contract

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

data class DetailUiState(
    val entry: Entry? = null,
    val entryType: EntryType = EntryType.LOGIN,
    val strategySummary: String = "",
    val validationError: String? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val strategyReady: Boolean = false,
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<String, String> = emptyMap(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    val history: List<EntryActivity> = emptyList(),
    val relatedEntries: List<Entry> = emptyList(),
    val isFaviconDownloading: Boolean = false
) {
    fun revealed(key: String): String? = revealedFields[key]
}

object RevealedFieldKey {
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val SSH_PRIVATE_KEY = "sshPrivateKey"
    const val SSH_PASSPHRASE = "sshPassphrase"
    const val CARDHOLDER = "cardholder"
    const val CARD_NUMBER = "cardNumber"
    const val CVV = "cvv"
    const val PAYMENT_PIN = "paymentPin"
    const val SEED_PHRASE = "seedPhrase"
    const val PASSKEY_DATA = "passkeyData"
    const val RECOVERY_CODES = "recoveryCodes"
    const val ID_NUMBER = "idNumber"
}

sealed interface DetailIntent {
    data class Initialize(val initialEntry: Entry) : DetailIntent
    data class SyncEntry(val entry: Entry) : DetailIntent
    data class CommitEntryUpdate(val entry: Entry) : DetailIntent

    object StartTitleEdit : DetailIntent
    object CancelTitleEdit : DetailIntent
    data class UpdateEditedTitle(val value: String) : DetailIntent

    object SaveTitle : DetailIntent
    object ToggleFavorite : DetailIntent

    data class RevealField(val key: String, val value: String?) : DetailIntent
    data class ToggleVisibility(val key: String) : DetailIntent
    data class SaveField(val key: String, val newValue: String) : DetailIntent
    data class RevealHighSensitivityField(val key: String) : DetailIntent
    data class RevealHighSensitivityFields(val keys: Set<String>) : DetailIntent
    data class DownloadFavicon(val domain: String) : DetailIntent

    data class RecordAction(val field: String, val type: ActivityType) : DetailIntent
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailIntent
    object ClearSensitiveState : DetailIntent
}

sealed interface DetailEffect {
    data class EntryUpdated(val entry: Entry) : DetailEffect
}

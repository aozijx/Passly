package com.aozijx.passly.feature.detail.contract

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry

data class DetailUiState(
    val entry: VaultEntry? = null,
    val vaultType: EntryType = EntryType.LOGIN,
    val strategySummary: String = "",
    val validationError: String? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val strategyReady: Boolean = false,
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<String, String> = emptyMap(),
    val history: List<EntryActivity> = emptyList()
) {
    fun revealed(key: String): String? = revealedFields[key]
}

object RevealedFieldKey {
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val SSH_PRIVATE_KEY = "sshPrivateKey"
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
    data class Initialize(val initialEntry: VaultEntry) : DetailIntent
    data class SyncEntry(val entry: VaultEntry) : DetailIntent
    data class CommitEntryUpdate(val entry: VaultEntry) : DetailIntent
    object ShowIconPicker : DetailIntent

    object StartTitleEdit : DetailIntent
    object CancelTitleEdit : DetailIntent
    data class UpdateEditedTitle(val value: String) : DetailIntent

    object SaveTitle : DetailIntent
    object ToggleFavorite : DetailIntent

    data class RevealField(val key: String, val value: String?) : DetailIntent

    data class RecordAction(val field: String, val type: ActivityType) : DetailIntent
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailIntent
    object ClearSensitiveState : DetailIntent
}

sealed interface DetailEffect {
    data class EntryUpdated(val entry: VaultEntry) : DetailEffect
    data object IconPickerRequested : DetailEffect
}

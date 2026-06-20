package com.aozijx.passly.ui.features.detail.contract

import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory

data class DetailUiState(
    val entry: VaultEntry? = null,
    val vaultType: EntryType = EntryType.PASSWORD,
    val strategySummary: String = "",
    val validationError: String? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val strategyReady: Boolean = false,
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<String, String> = emptyMap(),
    val history: List<VaultHistory> = emptyList()
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

sealed interface DetailEvent {
    data class Initialize(val initialEntry: VaultEntry) : DetailEvent
    data class SyncEntry(val entry: VaultEntry) : DetailEvent
    data class CommitEntryUpdate(val entry: VaultEntry) : DetailEvent
    object ShowIconPicker : DetailEvent

    object StartTitleEdit : DetailEvent
    object CancelTitleEdit : DetailEvent
    data class UpdateEditedTitle(val value: String) : DetailEvent

    object SaveTitle : DetailEvent
    object ToggleFavorite : DetailEvent

    data class RevealField(val key: String, val value: String?) : DetailEvent

    data class RecordAction(val field: String, val type: VaultHistory.HistoryType) : DetailEvent
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailEvent
    object ClearSensitiveState : DetailEvent
}

sealed interface DetailEffect {
    data class EntryUpdated(val entry: VaultEntry) : DetailEffect
    data object IconPickerRequested : DetailEffect
}
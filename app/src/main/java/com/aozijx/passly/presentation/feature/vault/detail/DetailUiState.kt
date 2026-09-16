package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel

data class DetailUiState(
    val entry: Entry? = null,
    val entryType: EntryType = EntryType.LOGIN,
    val strategySummary: String = "",
    val validationError: String? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val strategyReady: Boolean = false,
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<String, SensitiveValue> = emptyMap(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    val history: List<EntryActivity> = emptyList(),
    val relatedEntries: List<Entry> = emptyList(),
    val sections: List<DetailSectionKey> = emptyList(),
    val fieldEdits: DetailFieldEditState = DetailFieldEditState(),
    val savingEdit: DetailEditCompletion? = null,
    val completedEdit: DetailEditCompletion? = null,
    val saveCompletionId: Long = 0,
    val saveErrorCode: String? = null,
    val tagEditor: DetailTagEditorUiModel = DetailTagEditorUiModel(),
    val faviconEditor: DetailFaviconEditorUiModel = DetailFaviconEditorUiModel(),
) {
    fun revealed(key: String): SensitiveValue? = revealedFields[key]
}

data class DetailFieldEditState(
    private val drafts: Map<String, String> = emptyMap(),
) {
    fun isEditing(key: String): Boolean = key in drafts
    fun draft(key: String): String = drafts[key].orEmpty()

    fun start(key: String, initialValue: String): DetailFieldEditState =
        copy(drafts = drafts + (key to initialValue))

    fun update(key: String, value: String): DetailFieldEditState =
        if (key in drafts) copy(drafts = drafts + (key to value)) else this

    fun finish(key: String): DetailFieldEditState = copy(drafts = drafts - key)

    override fun toString(): String = "DetailFieldEditState(editingKeys=${drafts.keys})"
}

sealed interface DetailEditCompletion {
    data object Title : DetailEditCompletion
    data object Favorite : DetailEditCompletion
    data object Notes : DetailEditCompletion
    data object Associations : DetailEditCompletion
    data object Tags : DetailEditCompletion
    data object Icon : DetailEditCompletion
    data class SensitiveField(val key: String) : DetailEditCompletion
}

object DetailEditKey {
    const val NOTES = "notes"
    const val DOMAIN = "domain"
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

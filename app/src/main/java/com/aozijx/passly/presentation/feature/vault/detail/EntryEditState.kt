package com.aozijx.passly.presentation.feature.vault.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.domain.entry.model.Entry

/**
 * 条目详情页统一编辑状态
 */
class EntryEditState(initialEntry: Entry) {
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")
    var editedNotes by mutableStateOf(initialEntry.secret.notes.toTextFieldValue())
    var editedDomain by mutableStateOf(initialEntry.associations.primaryUrl ?: "")
    var editedPackage by mutableStateOf(initialEntry.associations.applicationIds.firstOrNull() ?: "")
    var editedTotpSecret by mutableStateOf(
        initialEntry.secret.otp?.config?.secret ?: ""
    )
    var editedTotp by mutableStateOf(
        initialEntry.secret.otp?.config?.secret ?: ""
    )

    // --- 字段编辑标志 ---
    var isEditingTitle by mutableStateOf(false)
    var isEditingCategory by mutableStateOf(false)
    var isEditingNotes by mutableStateOf(false)
    var isEditingDomain by mutableStateOf(false)
    var isEditingPackage by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var isEditingTotp by mutableStateOf(false)

    fun startNotesEditing(notes: String?) {
        editedNotes = notes.toTextFieldValue()
        isEditingNotes = true
    }

    private fun String?.toTextFieldValue(): TextFieldValue {
        val value = orEmpty()
        return TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }
}

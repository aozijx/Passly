package com.aozijx.passly.presentation.feature.vault.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.domain.entry.model.Entry

/** UI-local drafts whose cursor or independent field focus does not belong in the ViewModel. */
class DetailLocalEditState(initialEntry: Entry) {
    var editedNotes by mutableStateOf(initialEntry.secret.notes.toTextFieldValue())
    var editedDomain by mutableStateOf(initialEntry.associations.primaryUrl.orEmpty())
    var editedPackage by mutableStateOf(initialEntry.associations.applicationIds.firstOrNull().orEmpty())
    var isEditingNotes by mutableStateOf(false)
    var isEditingDomain by mutableStateOf(false)
    var isEditingPackage by mutableStateOf(false)

    fun startNotesEditing(notes: String?) {
        editedNotes = notes.toTextFieldValue()
        isEditingNotes = true
    }

    private fun String?.toTextFieldValue(): TextFieldValue {
        val value = orEmpty()
        return TextFieldValue(
            text = value,
            selection = TextRange(value.length),
        )
    }
}
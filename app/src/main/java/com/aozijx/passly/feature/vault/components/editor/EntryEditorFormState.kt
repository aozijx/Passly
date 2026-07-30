package com.aozijx.passly.feature.vault.components.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class EntryEditorFormState {
    private val values = mutableStateMapOf<EntryEditorFieldKey, String>()
    var isSecretVisible by mutableStateOf(false)

    val canSave: Boolean
        get() = value(EntryEditorFieldKey.TITLE).isNotBlank()

    fun value(key: EntryEditorFieldKey): String = values[key].orEmpty()

    fun update(key: EntryEditorFieldKey, value: String) {
        values[key] = value
    }
}

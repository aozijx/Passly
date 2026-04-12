package com.aozijx.passly.features.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.VaultEntry

/**
 * 条目详情页统一编辑状态（原 EntryEditState）
 */
class EntryEditState(initialEntry: VaultEntry) {
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")
    var editedCategory by mutableStateOf(initialEntry.category)
    var editedNotes by mutableStateOf(initialEntry.notes ?: "")
    var editedDomain by mutableStateOf(initialEntry.associatedDomain ?: "")
    var editedPackage by mutableStateOf(initialEntry.associatedAppPackage ?: "")
    var editedTotpSecret by mutableStateOf(initialEntry.totpSecret ?: "")
    var editedTotp by mutableStateOf(initialEntry.totpSecret ?: "")

    // --- 字段编辑标志 ---
    var isEditingTitle by mutableStateOf(false)
    var isEditingCategory by mutableStateOf(false)
    var isEditingNotes by mutableStateOf(false)
    var isEditingDomain by mutableStateOf(false)
    var isEditingPackage by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var isEditingTotp by mutableStateOf(false)

    fun applyTo(entry: VaultEntry): VaultEntry = entry.copy(
        title = editedTitle,
        category = editedCategory,
        notes = editedNotes.ifBlank { null },
        associatedDomain = editedDomain.ifBlank { null },
        associatedAppPackage = editedPackage.ifBlank { null },
        totpSecret = editedTotpSecret.ifBlank { null }
    )

    fun applyCategoryOnly(entry: VaultEntry): VaultEntry = entry.copy(category = editedCategory)

    fun applyTitleOnly(entry: VaultEntry): VaultEntry = entry.copy(title = editedTitle)

    fun applyNotesOnly(entry: VaultEntry): VaultEntry =
        entry.copy(notes = editedNotes.ifBlank { null })

    fun applyAssociatedOnly(entry: VaultEntry): VaultEntry = entry.copy(
        associatedDomain = editedDomain.ifBlank { null },
        associatedAppPackage = editedPackage.ifBlank { null }
    )
}

package com.aozijx.passly.core.designsystem.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.data.model.VaultEntry

/**
 * 统一的保险箱编辑状态管理器
 */
class VaultEditState(initialEntry: VaultEntry) {
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("") 
    var editedPassword by mutableStateOf("") 
    var editedCategory by mutableStateOf(initialEntry.category)
    var editedNotes by mutableStateOf(initialEntry.notes ?: "")
    var editedDomain by mutableStateOf(initialEntry.associatedDomain ?: "")
    var editedPackage by mutableStateOf(initialEntry.associatedAppPackage ?: "")
    var editedTotpSecret by mutableStateOf(initialEntry.totpSecret ?: "")

    // --- 交互状态标志 ---
    var isEditingCategory by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)

    fun applyTo(entry: VaultEntry): VaultEntry {
        return entry.copy(
            title = editedTitle,
            category = editedCategory,
            notes = editedNotes.ifBlank { null },
            associatedDomain = editedDomain.ifBlank { null },
            associatedAppPackage = editedPackage.ifBlank { null },
            totpSecret = editedTotpSecret.ifBlank { null }
        )
    }
}

package com.aozijx.passly.core.designsystem.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.VaultEntry

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
    var editedTotp by mutableStateOf(initialEntry.totpSecret ?: "")

    // --- 交互状态标志 ---
    var isEditingTitle by mutableStateOf(false)
    var isEditingCategory by mutableStateOf(false)
    var isEditingNotes by mutableStateOf(false)
    var isEditingDomain by mutableStateOf(false)
    var isEditingPackage by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var isEditingTotp by mutableStateOf(false)

    /**
     * 应用所有编辑到条目
     */
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
    
    /**
     * 只应用分类修改
     */
    fun applyCategoryOnly(entry: VaultEntry): VaultEntry {
        return entry.copy(category = editedCategory)
    }
    
    /**
     * 只应用标题修改
     */
    fun applyTitleOnly(entry: VaultEntry): VaultEntry {
        return entry.copy(title = editedTitle)
    }
    
    /**
     * 只应用备注修改
     */
    fun applyNotesOnly(entry: VaultEntry): VaultEntry {
        return entry.copy(notes = editedNotes.ifBlank { null })
    }
    
    /**
     * 只应用关联信息修改（域名和包名）
     */
    fun applyAssociatedOnly(entry: VaultEntry): VaultEntry {
        return entry.copy(
            associatedDomain = editedDomain.ifBlank { null },
            associatedAppPackage = editedPackage.ifBlank { null }
        )
    }
}



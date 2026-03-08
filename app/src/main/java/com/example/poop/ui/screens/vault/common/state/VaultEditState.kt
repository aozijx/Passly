package com.example.poop.ui.screens.vault.common.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.poop.data.VaultEntry

/**
 * 统一的保险箱编辑状态管理器
 * 整合了所有可能被编辑的字段，支持详情页、弹窗、自动填充确认等多个场景
 */
class VaultEditState(initialEntry: VaultEntry) {
    // 原始数据备份，用于脏检查或重置
    private var sourceEntry by mutableStateOf(initialEntry)

    // --- 可编辑字段状态 ---
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("") // 默认空，由外部解密后填充
    var editedPassword by mutableStateOf("") // 默认空，由外部解密后填充
    var editedCategory by mutableStateOf(initialEntry.category)
    var editedNotes by mutableStateOf(initialEntry.notes ?: "")
    var editedDomain by mutableStateOf(initialEntry.associatedDomain ?: "")
    var editedPackage by mutableStateOf(initialEntry.associatedAppPackage ?: "")
    var editedTotpSecret by mutableStateOf(initialEntry.totpSecret ?: "")

    // --- 交互状态标志 ---
    var isEditingCategory by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var isEditingNotes by mutableStateOf(false)
    var isEditingDetails by mutableStateOf(false) // 针对域名/包名等详情的编辑模式

    /**
     * 脏检查：判断当前编辑的内容是否与原始数据（或已解密的数据）不一致
     * 注意：用户名和密码通常需要解密后比较，这里提供基础逻辑
     */
    val isDirty by derivedStateOf {
        editedTitle != sourceEntry.title ||
        editedCategory != sourceEntry.category ||
        editedNotes != (sourceEntry.notes ?: "") ||
        editedDomain != (sourceEntry.associatedDomain ?: "") ||
        editedPackage != (sourceEntry.associatedAppPackage ?: "") ||
        editedTotpSecret != (sourceEntry.totpSecret ?: "")
    }

    /**
     * 当切换显示的条目时，更新状态
     */
    fun updateFrom(entry: VaultEntry) {
        sourceEntry = entry
        editedTitle = entry.title
        editedCategory = entry.category
        editedNotes = entry.notes ?: ""
        editedDomain = entry.associatedDomain ?: ""
        editedPackage = entry.associatedAppPackage ?: ""
        editedTotpSecret = entry.totpSecret ?: ""
        
        // 重置所有编辑状态
        isEditingCategory = false
        isEditingUsername = false
        isEditingPassword = false
        isEditingNotes = false
        isEditingDetails = false
    }

    /**
     * 辅助方法：将当前编辑的状态应用回 Entry 对象
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
}

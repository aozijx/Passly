package com.example.poop.common.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.poop.data.model.VaultEntry

/**
 * 统一的保险箱编辑状态管理器
 */
class VaultEditState(initialEntry: VaultEntry) {
    private var sourceEntry by mutableStateOf(initialEntry)
    
    // 记录解密后的初始明文，用于精准的 isDirty 检查
    private var originalDecryptedUsername by mutableStateOf("")
    private var originalDecryptedPassword by mutableStateOf("")

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
    var isEditingNotes by mutableStateOf(false)
    var isEditingDetails by mutableStateOf(false)
    var isPasswordVisible by mutableStateOf(false)

    /**
     * 精准脏检查：只要任何一个字段与初始状态不同，即返回 true
     */
    val isDirty by derivedStateOf {
        editedTitle != sourceEntry.title ||
        editedCategory != sourceEntry.category ||
        editedUsername != originalDecryptedUsername ||
        editedPassword != originalDecryptedPassword ||
        editedNotes != (sourceEntry.notes ?: "") ||
        editedDomain != (sourceEntry.associatedDomain ?: "") ||
        editedPackage != (sourceEntry.associatedAppPackage ?: "") ||
        editedTotpSecret != (sourceEntry.totpSecret ?: "")
    }

    /**
     * 外部解密成功后，调用此方法填充初始明文
     */
    fun setDecryptedContent(username: String?, password: String?) {
        val u = username ?: ""
        val p = password ?: ""
        originalDecryptedUsername = u
        originalDecryptedPassword = p
        editedUsername = u
        editedPassword = p
    }

    fun updateFrom(entry: VaultEntry) {
        sourceEntry = entry
        editedTitle = entry.title
        editedCategory = entry.category
        editedNotes = entry.notes ?: ""
        editedDomain = entry.associatedDomain ?: ""
        editedPackage = entry.associatedAppPackage ?: ""
        editedTotpSecret = entry.totpSecret ?: ""
        
        // 重置明文备份
        originalDecryptedUsername = ""
        originalDecryptedPassword = ""
        editedUsername = ""
        editedPassword = ""

        isEditingCategory = false
        isEditingUsername = false
        isEditingPassword = false
        isEditingNotes = false
        isEditingDetails = false
        isPasswordVisible = false
    }

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

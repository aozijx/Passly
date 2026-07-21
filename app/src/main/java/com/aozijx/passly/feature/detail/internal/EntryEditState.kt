package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo

/**
 * 条目详情页统一编辑状态（原 EntryEditState）
 */
class EntryEditState(initialEntry: VaultEntry) {
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")
    var editedCategory by mutableStateOf(initialEntry.category)
    var editedNotes by mutableStateOf(initialEntry.credential.notes ?: "")
    var editedDomain by mutableStateOf(initialEntry.associatedDomain ?: "")
    var editedPackage by mutableStateOf(initialEntry.associatedAppPackage ?: "")
    var editedTotpSecret by mutableStateOf(initialEntry.credential.otp?.secret ?: "")
    var editedTotp by mutableStateOf(initialEntry.credential.otp?.secret ?: "")

    // --- 字段编辑标志 ---
    var isEditingTitle by mutableStateOf(false)
    var isEditingCategory by mutableStateOf(false)
    var isEditingNotes by mutableStateOf(false)
    var isEditingDomain by mutableStateOf(false)
    var isEditingPackage by mutableStateOf(false)
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var isEditingTotp by mutableStateOf(false)

    fun applyTo(entry: VaultEntry): VaultEntry {
        val newMetadata = entry.metadata.copy(
            title = editedTitle,
            entryType = runCatching { EntryType.valueOf(editedCategory) }.getOrDefault(entry.metadata.entryType),
            website = buildWebsite(entry.metadata.website)
        )
        val newCredential = entry.credential.copy(
            notes = editedNotes.ifBlank { null },
            otp = updateTwoFactorSecret(entry.credential.otp, editedTotpSecret.ifBlank { null })
        )
        return entry.copy(metadata = newMetadata, credential = newCredential)
    }

    fun applyCategoryOnly(entry: VaultEntry): VaultEntry = entry.copy(
        metadata = entry.metadata.copy(
            entryType = runCatching { EntryType.valueOf(editedCategory) }.getOrDefault(entry.metadata.entryType)
        )
    )

    fun applyTitleOnly(entry: VaultEntry): VaultEntry = entry.copy(
        metadata = entry.metadata.copy(title = editedTitle)
    )

    fun applyNotesOnly(entry: VaultEntry): VaultEntry =
        entry.copy(credential = entry.credential.copy(notes = editedNotes.ifBlank { null }))

    fun applyAssociatedOnly(entry: VaultEntry): VaultEntry = entry.copy(
        metadata = entry.metadata.copy(website = buildWebsite(entry.metadata.website))
    )

    private fun buildWebsite(existing: WebsiteInfo?): WebsiteInfo? {
        val domain = editedDomain.ifBlank { null }
        val pkg = editedPackage.ifBlank { null }
        if (domain == null && pkg == null && existing == null) return null
        return (existing ?: WebsiteInfo()).copy(
            primaryUrl = domain,
            packageNames = if (pkg == null) existing?.packageNames ?: emptySet() else setOf(pkg)
        )
    }

    private fun updateTwoFactorSecret(existing: OtpConfig?, secret: String?): OtpConfig? {
        if (secret == null) return null
        return existing?.copy(secret = secret)
    }
}

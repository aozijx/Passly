package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.secret.OtpSecret

/**
 * 条目详情页统一编辑状态（原 EntryEditState）
 */
class EntryEditState(initialEntry: VaultEntry) {
    var editedTitle by mutableStateOf(initialEntry.title)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")
    var editedCategory by mutableStateOf(initialEntry.category)
    var editedNotes by mutableStateOf(
        initialEntry.secret.notes ?: ""
    )
    var editedDomain by mutableStateOf(initialEntry.associatedDomain ?: "")
    var editedPackage by mutableStateOf(initialEntry.associatedAppPackage ?: "")
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

    fun applyTo(entry: VaultEntry): VaultEntry {
        val newSummary = entry.summary.copy(
            title = editedTitle,
            website = buildWebsite(entry.summary.website)
        )
        val newHeader = entry.header.copy(
            entryType = runCatching { EntryType.valueOf(editedCategory) }.getOrDefault(entry.header.entryType)
        )
        val newSecret = updateSecret(entry.secret)
        return entry.copy(summary = newSummary, header = newHeader, secret = newSecret)
    }

    fun applyCategoryOnly(entry: VaultEntry): VaultEntry = entry.copy(
        header = entry.header.copy(
            entryType = runCatching { EntryType.valueOf(editedCategory) }.getOrDefault(entry.header.entryType)
        )
    )

    fun applyTitleOnly(entry: VaultEntry): VaultEntry = entry.copy(
        summary = entry.summary.copy(title = editedTitle)
    )

    fun applyNotesOnly(entry: VaultEntry): VaultEntry =
        entry.copy(secret = updateSecretNotes(entry.secret))

    fun applyAssociatedOnly(entry: VaultEntry): VaultEntry = entry.copy(
        summary = entry.summary.copy(website = buildWebsite(entry.summary.website))
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

    private fun updateSecret(secret: EntrySecret): EntrySecret {
        val notes = editedNotes.ifBlank { null }
        val totpSecret = editedTotpSecret.ifBlank { null }
        val currentOtpData = secret.otp
        return if (totpSecret != null || currentOtpData != null) {
            val newConfig = if (totpSecret != null) {
                (currentOtpData?.config ?: OtpConfig(secret = totpSecret)).copy(secret = totpSecret)
            } else {
                currentOtpData?.config?.copy(secret = "") ?: OtpConfig(secret = "")
            }
            secret.copy(
                notes = notes,
                otp = (currentOtpData ?: OtpSecret()).copy(config = newConfig)
            )
        } else {
            secret.copy(notes = notes)
        }
    }

    private fun updateSecretNotes(secret: EntrySecret): EntrySecret {
        val notes = editedNotes.ifBlank { null }
        return secret.copy(notes = notes)
    }
}

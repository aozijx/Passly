package com.aozijx.passly.feature.detail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.credential.OtpCredential

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

    fun applyTo(entry: Entry): Entry {
        val newProfile = entry.profile.copy(
            title = editedTitle,
            associations = buildAssociations(entry.profile.associations)
        )
        val newSecret = updateSecret(entry.secret)
        return entry.copy(profile = newProfile, secret = newSecret)
    }

    fun applyTitleOnly(entry: Entry): Entry = entry.copy(
        profile = entry.profile.copy(title = editedTitle)
    )

    fun applyNotesOnly(entry: Entry): Entry =
        entry.copy(secret = updateSecretNotes(entry.secret))

    fun applyAssociatedOnly(entry: Entry): Entry = entry.copy(
        profile = entry.profile.copy(associations = buildAssociations(entry.profile.associations))
    )

    fun startNotesEditing(notes: String?) {
        editedNotes = notes.toTextFieldValue()
        isEditingNotes = true
    }

    private fun buildAssociations(existing: EntryAssociations): EntryAssociations {
        val domain = editedDomain.ifBlank { null }
        val pkg = editedPackage.ifBlank { null }
        return existing.copy(
            primaryUrl = domain,
            applicationIds = if (pkg == null) existing.applicationIds else setOf(pkg)
        )
    }

    private fun updateSecret(secret: EntrySecret): EntrySecret {
        val notes = editedNotes.text.ifBlank { null }
        val totpSecret = editedTotpSecret.ifBlank { null }
        val currentOtpData = secret.otp
        return if (totpSecret != null || currentOtpData != null) {
            val newConfig = if (totpSecret != null) {
                (currentOtpData?.config ?: OtpConfig(secret = totpSecret)).copy(secret = totpSecret)
            } else {
                // 未编辑 OTP secret 时置 null，保存路径会保留旧字段级值。
                currentOtpData?.config?.copy(secret = null) ?: OtpConfig(secret = null)
            }
            secret.copy(
                notes = notes,
                credential = OtpCredential(config = newConfig)
            )
        } else {
            secret.copy(notes = notes)
        }
    }

    private fun updateSecretNotes(secret: EntrySecret): EntrySecret {
        val notes = editedNotes.text.ifBlank { null }
        return secret.copy(notes = notes)
    }

    private fun String?.toTextFieldValue(): TextFieldValue {
        val value = orEmpty()
        return TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }
}

package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential

/** Exactly one typed credential plus extensions shared by every entry type. */
data class EntrySecret(
    val credential: EntryCredential = EntryCredential.None,
    val notes: String? = null,
    val customFields: List<CustomField> = emptyList(),
) {
    val isEmpty: Boolean
        get() = credential === EntryCredential.None && notes.isNullOrBlank() && customFields.isEmpty()

    val login: LoginCredential? get() = credential as? LoginCredential
    val card: CardCredential? get() = credential as? CardCredential
    val identity: IdentityCredential? get() = credential as? IdentityCredential
    val ssh: SshCredential? get() = credential as? SshCredential
    val wifi: WifiCredential? get() = credential as? WifiCredential
    val passkey: PasskeyCredential? get() = credential as? PasskeyCredential
    val otp: OtpCredential? get() = credential as? OtpCredential
}

package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential

sealed interface DetailEntryPatch {
    fun applyTo(entry: Entry): Entry

    data class Title(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(title = value),
        )
    }

    data class Favorite(val value: Boolean) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(favorite = value),
        )
    }

    data class Username(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(username = value),
        )
    }

    data class LoginPassword(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(
                credential = entry.secret.login?.copy(password = value)
                    ?: LoginCredential(password = value),
            ),
        )
    }

    data class CardNumber(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(
                credential = (entry.secret.card ?: CardCredential()).copy(cardNumber = value),
            ),
        )
    }

    data class CardCvv(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(
                credential = (entry.secret.card ?: CardCredential()).copy(cardCvv = value),
            ),
        )
    }

    data class WifiPassword(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(
                credential = requireNotNull(entry.secret.wifi).copy(password = value),
            ),
        )
    }

    data class SshPassphrase(val value: String) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(
                credential = requireNotNull(entry.secret.ssh).copy(passphrase = value),
            ),
        )
    }

    data class Notes(val value: String?) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            secret = entry.secret.copy(notes = value),
        )
    }

    data class Associations(
        val primaryUrl: String?,
        val applicationIds: Set<String>,
    ) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(
                associations = entry.associations.copy(
                    primaryUrl = primaryUrl,
                    applicationIds = applicationIds,
                ),
            ),
        )
    }

    data class Tags(val values: Set<String>) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(tags = values),
        )
    }

    data class Icon(val value: EntryIcon) : DetailEntryPatch {
        override fun applyTo(entry: Entry): Entry = entry.copy(
            profile = entry.profile.copy(icon = value),
        )
    }
}

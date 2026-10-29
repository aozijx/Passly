package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Bitwarden plaintext export wire models; conversion to Passly archive records stays in the adapter. */
@Serializable
internal data class BitwardenExport(
    val encrypted: Boolean = false,
    val folders: List<BitwardenFolder> = emptyList(),
    val items: List<BitwardenItem>,
)

@Serializable
internal data class BitwardenFolder(
    val id: String,
    val name: String,
)

@Serializable
internal data class BitwardenItem(
    val id: String? = null,
    val organizationId: String? = null,
    val folderId: String? = null,
    val collectionIds: List<String> = emptyList(),
    val type: Int,
    val reprompt: Int = 0,
    val name: String,
    val notes: String? = null,
    val favorite: Boolean = false,
    val fields: List<BitwardenField> = emptyList(),
    val login: BitwardenLogin? = null,
    val card: BitwardenCard? = null,
    val identity: BitwardenIdentity? = null,
    val attachments: List<JsonObject> = emptyList(),
    val passwordHistory: List<JsonObject> = emptyList(),
    val creationDate: String? = null,
    val revisionDate: String? = null,
    val deletedDate: String? = null,
)

@Serializable
internal data class BitwardenField(
    val name: String? = null,
    val value: String? = null,
    val type: Int? = null,
)

@Serializable
internal data class BitwardenLogin(
    val uris: List<BitwardenUri> = emptyList(),
    val username: String? = null,
    val password: String? = null,
    val totp: String? = null,
    val fido2Credentials: List<JsonObject> = emptyList(),
)

@Serializable
internal data class BitwardenUri(
    val uri: String? = null,
)

@Serializable
internal data class BitwardenCard(
    val cardholderName: String? = null,
    val brand: String? = null,
    val number: String? = null,
    val expMonth: String? = null,
    val expYear: String? = null,
    val code: String? = null,
)

@Serializable
internal data class BitwardenIdentity(
    val title: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val address3: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val company: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val ssn: String? = null,
    val username: String? = null,
    val passportNumber: String? = null,
    val licenseNumber: String? = null,
)

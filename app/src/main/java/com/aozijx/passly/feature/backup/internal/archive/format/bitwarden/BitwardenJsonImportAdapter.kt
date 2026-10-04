package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.containsAscii
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.model.BackupFormatId
import com.aozijx.passly.feature.backup.internal.model.BackupFormats
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLDecoder
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports Bitwarden plaintext JSON exports into Passly's canonical bundle.
 *
 * Encrypted/account-restricted exports, attachments ZIPs, SSH keys, and FIDO2
 * credentials are rejected instead of being silently truncated.
 */
@Singleton
internal class BitwardenJsonImportAdapter @Inject constructor() : BackupImportAdapter {
    override val formatId: BackupFormatId = BackupFormats.BITWARDEN_JSON
    override val requiresPassword: Boolean = false

    override fun probe(payload: ByteArray): Int =
        if (
            (
                    payload.containsAscii("\"items\"") ||
                            (
                                    payload.containsAscii("\"encrypted\"") &&
                                            payload.containsAscii("\"data\"")
                                    )
                    )
        ) 70 else 0

    override fun decode(
        payload: ByteArray,
        password: CharArray?,
        strategy: BackupImportStrategy,
    ): BackupBundle =
        try {
            decodeValidated(payload)
        } catch (error: BackupFailed) {
            throw error
        } catch (error: Exception) {
            throw BackupFailed()
        }

    private fun decodeValidated(payload: ByteArray): BackupBundle {
        val rawRoot =
            BackupJson.parseToJsonElement(payload.decodeStrictUtf8("Bitwarden JSON")).jsonObject
        if (rawRoot["encrypted"]?.jsonPrimitive?.booleanOrNull == true) {
            throw BackupFailed()
        }
        val export = BackupJson.decodeFromString<BitwardenExport>(rawRoot.toString())
        val folderNames = export.folders.associate { it.id to it.name }
        val now = System.currentTimeMillis()
        val importedRecords = export.items.mapIndexed { index, item ->
            if (item.type !in 1..4) {
                throw BackupFailed()
            }
            if (!item.login?.fido2Credentials.isNullOrEmpty()) {
                throw BackupFailed()
            }
            if (item.attachments.isNotEmpty()) {
                throw BackupFailed()
            }
            if (item.passwordHistory.isNotEmpty()) {
                throw BackupFailed()
            }
            item.toBackupRecords(index, folderNames, now)
        }
        val bundle = BackupBundle(
            document = BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = now,
                appVersion = "Bitwarden plaintext JSON",
                entries = importedRecords.flatMap(BitwardenRecords::entries),
                links = importedRecords.flatMap(BitwardenRecords::links)
            )
        )
        BackupBundleValidator.validate(bundle, requireResourceData = false)
        return bundle
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun BitwardenItem.toBackupRecords(
        index: Int,
        folderNames: Map<String, String>,
        fallbackTime: Long,
    ): BitwardenRecords {
        val updatedAt = parseTime(revisionDate) ?: fallbackTime
        val createdAt = (parseTime(creationDate) ?: updatedAt).coerceAtMost(updatedAt)
        val title = name.ifBlank { "Bitwarden 条目 ${index + 1}" }
        val fields = mutableListOf(
            archiveField(FieldKey.TITLE, BackupFieldValue.text(title)),
            archiveField(FieldKey.FAVORITE, BackupFieldValue.boolean(favorite)),
        )
        login?.username?.takeIf(String::isNotBlank)?.let {
            fields += archiveField(FieldKey.USERNAME, BackupFieldValue.text(it))
        }
        login?.uris?.firstOrNull()?.uri?.takeIf(String::isNotBlank)?.let {
            fields += archiveField(FieldKey.PRIMARY_URL, BackupFieldValue.text(it))
        }
        folderId?.let(folderNames::get)?.takeIf(String::isNotBlank)?.let {
            fields += archiveField(FieldKey.TAGS, BackupFieldValue.texts(listOf(it)))
        }
        notes?.takeIf(String::isNotBlank)?.let {
            fields += archiveField(FieldKey.NOTES, BackupFieldValue.text(it))
        }

        val customFields = mutableListOf<BackupCustomFieldValue>()
        this.fields.forEach { field ->
            val fieldName = field.name?.takeIf(String::isNotBlank) ?: return@forEach
            customFields += BackupCustomFieldValue(
                name = fieldName,
                value = field.value.orEmpty(),
                kind = if (field.type == 1) BackupCustomFieldValue.KIND_HIDDEN
                else BackupCustomFieldValue.KIND_TEXT,
            )
        }
        organizationId?.let {
            customFields += BackupCustomFieldValue("Bitwarden.organizationId", it, BackupCustomFieldValue.KIND_TEXT)
        }
        collectionIds.forEach {
            customFields += BackupCustomFieldValue("Bitwarden.collectionId", it, BackupCustomFieldValue.KIND_TEXT)
        }
        if (reprompt != 0) {
            customFields += BackupCustomFieldValue("Bitwarden.reprompt", reprompt.toString(), BackupCustomFieldValue.KIND_TEXT)
        }

        val entryType = when (type) {
            1 -> EntryType.LOGIN
            2 -> EntryType.NOTE
            3 -> EntryType.BANK_CARD
            4 -> EntryType.ID_CARD
            else -> error("validated above")
        }
        when (entryType) {
            EntryType.LOGIN -> {
                login?.password?.takeIf(String::isNotBlank)?.let {
                    fields += archiveField(FieldKey.PASSWORD, BackupFieldValue.text(it))
                }
            }
            EntryType.BANK_CARD -> card?.let { source ->
                source.number?.takeIf(String::isNotBlank)?.let {
                    fields += archiveField(FieldKey.CARD_NUMBER, BackupFieldValue.text(it))
                }
                listOfNotNull(source.expMonth, source.expYear)
                    .takeIf(List<String>::isNotEmpty)?.joinToString("/")?.let {
                        fields += archiveField(FieldKey.CARD_EXPIRATION, BackupFieldValue.text(it))
                    }
                source.code?.takeIf(String::isNotBlank)?.let {
                    fields += archiveField(FieldKey.CARD_CVV, BackupFieldValue.text(it))
                }
                source.cardholderName?.takeIf(String::isNotBlank)?.let {
                    fields += archiveField(FieldKey.CARD_HOLDER, BackupFieldValue.text(it))
                }
                source.brand?.takeIf(String::isNotBlank)?.let {
                    fields += archiveField(FieldKey.PAYMENT_PLATFORM, BackupFieldValue.text(it))
                }
            }
            EntryType.ID_CARD -> identity?.let { source ->
                val idNumber = source.ssn ?: source.passportNumber ?: source.licenseNumber
                    ?: throw BackupFailed()
                fields += archiveField(FieldKey.ID_NUMBER, BackupFieldValue.text(idNumber))
                listOf(
                    "称谓" to source.title,
                    "名字" to source.firstName,
                    "中间名" to source.middleName,
                    "姓氏" to source.lastName,
                    "公司" to source.company,
                    "邮箱" to source.email,
                    "电话" to source.phone,
                    "用户名" to source.username,
                    "地址1" to source.address1,
                    "地址2" to source.address2,
                    "地址3" to source.address3,
                    "城市" to source.city,
                    "州/省" to source.state,
                    "邮编" to source.postalCode,
                    "国家" to source.country,
                ).forEach { (label, value) ->
                    value?.takeIf(String::isNotBlank)?.let {
                        customFields += BackupCustomFieldValue("Bitwarden.$label", it, BackupCustomFieldValue.KIND_TEXT)
                    }
                }
            }
            else -> Unit
        }
        if (customFields.isNotEmpty()) {
            fields += archiveField(FieldKey.CUSTOM_FIELDS, BackupFieldValue.customFields(customFields))
        }
        val record = BackupEntryRecord(
            id = safeId(id, index),
            type = BackupArchiveKeyRegistry.entryTypeKey(entryType),
            revision = 1,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = parseTime(deletedDate),
            fields = fields,
        )
        val otpFields = login?.totp?.takeIf(String::isNotBlank)?.let(::parseOtpFields)
            ?: return BitwardenRecords(entries = listOf(record))
        val accountId = relatedId(record.id, "account")
        val otpId = relatedId(record.id, "otp")
        val account = BackupEntryRecord(
            id = accountId,
            type = BackupArchiveKeyRegistry.entryTypeKey(EntryType.ACCOUNT),
            revision = 1,
            createdAt = createdAt,
            updatedAt = updatedAt,
            fields = listOf(
                archiveField(FieldKey.TITLE, BackupFieldValue.text(title)),
                archiveField(FieldKey.FAVORITE, BackupFieldValue.boolean(favorite)),
            ),
        )
        val otpEntry = BackupEntryRecord(
            id = otpId,
            type = BackupArchiveKeyRegistry.entryTypeKey(EntryType.OTP),
            revision = 1,
            createdAt = createdAt,
            updatedAt = updatedAt,
            fields = listOf(archiveField(FieldKey.TITLE, BackupFieldValue.text("$title OTP"))) + otpFields,
        )
        return BitwardenRecords(
            entries = listOf(account, record, otpEntry),
            links = listOf(
                BackupLinkRecord(
                    id = relatedId(record.id, "member-link"),
                    sourceEntryId = record.id,
                    targetEntryId = account.id,
                    relationType = BackupArchiveKeyRegistry.relationTypeKey(EntryRelationType.MEMBER_OF_ACCOUNT),
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                ),
                BackupLinkRecord(
                    id = relatedId(record.id, "otp-link"),
                    sourceEntryId = otpEntry.id,
                    targetEntryId = record.id,
                    relationType = BackupArchiveKeyRegistry.relationTypeKey(EntryRelationType.OTP_FOR),
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                ),
            ),
        )
    }

    private fun parseOtpFields(value: String): List<BackupFieldRecord> {
        val trimmed = value.trim()
        if (trimmed.startsWith("steam://", ignoreCase = true)) {
            return listOf(
                archiveField(FieldKey.OTP_TYPE, BackupFieldValue.text("steam")),
                archiveField(FieldKey.OTP_SECRET, BackupFieldValue.text(trimmed.substringAfter("steam://"))),
                archiveField(FieldKey.OTP_ALGORITHM, BackupFieldValue.text("sha1")),
                archiveField(FieldKey.OTP_DIGITS, BackupFieldValue.integer(5)),
                archiveField(FieldKey.OTP_PERIOD, BackupFieldValue.integer(30)),
                archiveField(FieldKey.OTP_ENCODING, BackupFieldValue.text("base32")),
            )
        }
        if (!trimmed.startsWith("otpauth://", ignoreCase = true)) {
            return listOf(
                archiveField(FieldKey.OTP_TYPE, BackupFieldValue.text("totp")),
                archiveField(FieldKey.OTP_SECRET, BackupFieldValue.text(trimmed.filterNot(Char::isWhitespace))),
                archiveField(FieldKey.OTP_ALGORITHM, BackupFieldValue.text("sha1")),
                archiveField(FieldKey.OTP_DIGITS, BackupFieldValue.integer(6)),
                archiveField(FieldKey.OTP_PERIOD, BackupFieldValue.integer(30)),
                archiveField(FieldKey.OTP_ENCODING, BackupFieldValue.text("base32")),
            )
        }
        val uri = URI(trimmed)
        val query = uri.rawQuery.orEmpty().split('&').filter(String::isNotBlank).associate { part ->
            decode(part.substringBefore('=')) to decode(part.substringAfter('=', ""))
        }
        val label = decode(uri.rawPath.orEmpty().removePrefix("/"))
        val labelParts = label.split(':', limit = 2)
        val typeKey = when (uri.host?.lowercase()) {
            "hotp" -> "hotp"
            "totp" -> "totp"
            else -> throw BackupFailed()
        }
        val secret = query["secret"]?.takeIf(String::isNotBlank) ?: throw BackupFailed()
        val algorithm = when (query["algorithm"]?.lowercase() ?: "sha1") {
            "sha1" -> "sha1"
            "sha256" -> "sha256"
            "sha512" -> "sha512"
            else -> throw BackupFailed()
        }
        return buildList {
            add(archiveField(FieldKey.OTP_TYPE, BackupFieldValue.text(typeKey)))
            add(archiveField(FieldKey.OTP_SECRET, BackupFieldValue.text(secret)))
            add(archiveField(FieldKey.OTP_ALGORITHM, BackupFieldValue.text(algorithm)))
            add(archiveField(FieldKey.OTP_DIGITS, BackupFieldValue.integer(query["digits"]?.toIntOrNull() ?: 6)))
            if (typeKey == "hotp") {
                add(archiveField(FieldKey.OTP_COUNTER, BackupFieldValue.long(query["counter"]?.toLongOrNull() ?: 0)))
            } else {
                add(archiveField(FieldKey.OTP_PERIOD, BackupFieldValue.integer(query["period"]?.toIntOrNull() ?: 30)))
            }
            add(archiveField(FieldKey.OTP_ENCODING, BackupFieldValue.text("base32")))
            (query["issuer"] ?: labelParts.getOrNull(0))?.takeIf(String::isNotBlank)?.let {
                add(archiveField(FieldKey.OTP_ISSUER, BackupFieldValue.text(it)))
            }
            labelParts.getOrNull(1)?.takeIf(String::isNotBlank)?.let {
                add(archiveField(FieldKey.OTP_ACCOUNT_NAME, BackupFieldValue.text(it)))
            }
        }
    }

    private fun archiveField(key: FieldKey, value: BackupFieldValue) = BackupFieldRecord(
        key = BackupArchiveKeyRegistry.fieldKey(key),
        value = value,
    )

    private fun parseTime(value: String?): Long? =
        value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private fun safeId(value: String?, index: Int): String {
        val candidate = value?.trim()
        if (candidate != null && candidate.matches(Regex("[A-Za-z0-9_-]{1,160}"))) return candidate
        return UUID.nameUUIDFromBytes(
            "bitwarden:${candidate.orEmpty()}:$index".toByteArray(Charsets.UTF_8)
        ).toString()
    }

    private fun relatedId(entryId: String, role: String): String =
        UUID.nameUUIDFromBytes(
            "bitwarden:$entryId:$role".toByteArray(Charsets.UTF_8)
        ).toString()

}

private data class BitwardenRecords(
    val entries: List<BackupEntryRecord>,
    val links: List<BackupLinkRecord> = emptyList()
)

@Serializable
private data class BitwardenExport(
    val encrypted: Boolean = false,
    val folders: List<BitwardenFolder> = emptyList(),
    val items: List<BitwardenItem>
)

@Serializable
private data class BitwardenFolder(
    val id: String,
    val name: String
)

@Serializable
private data class BitwardenItem(
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
    val attachments: List<kotlinx.serialization.json.JsonObject> = emptyList(),
    val passwordHistory: List<kotlinx.serialization.json.JsonObject> = emptyList(),
    val creationDate: String? = null,
    val revisionDate: String? = null,
    val deletedDate: String? = null
)

@Serializable
private data class BitwardenField(
    val name: String? = null,
    val value: String? = null,
    val type: Int? = null
)

@Serializable
private data class BitwardenLogin(
    val uris: List<BitwardenUri> = emptyList(),
    val username: String? = null,
    val password: String? = null,
    val totp: String? = null,
    val fido2Credentials: List<kotlinx.serialization.json.JsonObject> = emptyList()
)

@Serializable
private data class BitwardenUri(
    val uri: String? = null
)

@Serializable
private data class BitwardenCard(
    val cardholderName: String? = null,
    val brand: String? = null,
    val number: String? = null,
    val expMonth: String? = null,
    val expYear: String? = null,
    val code: String? = null
)

@Serializable
private data class BitwardenIdentity(
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
    val licenseNumber: String? = null
)

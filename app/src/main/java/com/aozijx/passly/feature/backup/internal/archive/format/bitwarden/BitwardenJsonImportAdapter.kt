package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.containsAscii
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCardSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupIdentitySecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLoginSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpAlgorithm
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpConfig
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSecretRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSummaryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupWebsiteRecord
import com.aozijx.passly.domain.backup.model.BackupFormatId
import com.aozijx.passly.domain.backup.model.BackupFormats
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
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

    override fun decode(payload: ByteArray, password: CharArray?): BackupBundle =
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

    private fun BitwardenItem.toBackupRecords(
        index: Int,
        folderNames: Map<String, String>,
        fallbackTime: Long
    ): BitwardenRecords {
        val updatedAt = parseTime(revisionDate) ?: fallbackTime
        val createdAt = (parseTime(creationDate) ?: updatedAt).coerceAtMost(updatedAt)
        val tags = folderId?.let(folderNames::get)?.let(::listOf).orEmpty()
        val customFields = fields.mapNotNull { field ->
            val name = field.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            BackupCustomField(name, field.value.orEmpty(), field.type ?: 0)
        }.toMutableList()
        organizationId?.let {
            customFields += BackupCustomField("Bitwarden.organizationId", it)
        }
        collectionIds.forEach {
            customFields += BackupCustomField("Bitwarden.collectionId", it)
        }
        if (reprompt != 0) {
            customFields += BackupCustomField("Bitwarden.reprompt", reprompt.toString())
        }

        val entryType = when (type) {
            1 -> "LOGIN"
            2 -> "NOTE"
            3 -> "CARD"
            4 -> "IDENTITY"
            else -> error("validated above")
        }
        val website = login?.uris?.firstOrNull()?.uri?.takeIf(String::isNotBlank)
            ?.let { BackupWebsiteRecord(primaryUrl = it) }
        val identitySecret = identity?.let {
            listOf(
                "称谓" to it.title,
                "名字" to it.firstName,
                "中间名" to it.middleName,
                "姓氏" to it.lastName,
                "公司" to it.company,
                "邮箱" to it.email,
                "电话" to it.phone,
                "用户名" to it.username,
                "地址1" to it.address1,
                "地址2" to it.address2,
                "地址3" to it.address3,
                "城市" to it.city,
                "州/省" to it.state,
                "邮编" to it.postalCode,
                "国家" to it.country,
                "护照号" to it.passportNumber,
                "驾照号" to it.licenseNumber
            ).forEach { (label, value) ->
                if (!value.isNullOrBlank()) {
                    customFields += BackupCustomField("Bitwarden.$label", value)
                }
            }
            BackupIdentitySecret(
                idNumber = it.ssn ?: it.passportNumber ?: it.licenseNumber
            )
        }
        val cardSecret = card?.let {
            val expiry = listOfNotNull(it.expMonth, it.expYear)
                .takeIf(List<String>::isNotEmpty)
                ?.joinToString("/")
            BackupCardSecret(
                cardNumber = it.number,
                cardExpiry = expiry,
                cardCvv = it.code,
                cardHolder = it.cardholderName,
                paymentPlatform = it.brand
            )
        }

        val record = BackupEntryRecord(
            id = safeId(id, index),
            type = entryType,
            version = 1,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = parseTime(deletedDate),
            summary = BackupSummaryRecord(
                title = name.ifBlank { "Bitwarden 条目 ${index + 1}" },
                username = login?.username.orEmpty(),
                website = website,
                favorite = favorite,
                tags = tags
            ),
            secret = BackupSecretRecord(
                login = login?.let {
                    BackupLoginSecret(password = it.password)
                },
                notes = notes,
                card = cardSecret,
                identity = identitySecret,
                otp = login?.totp?.takeIf(String::isNotBlank)?.let(::parseOtp),
                customFields = customFields
            )
        )
        val otp = record.secret.otp ?: return BitwardenRecords(entries = listOf(record))
        val accountId = relatedId(record.id, "account")
        val otpId = relatedId(record.id, "otp")
        val account = record.copy(
            id = accountId,
            type = "ACCOUNT",
            secret = BackupSecretRecord()
        )
        val login = record.copy(
            secret = record.secret.copy(otp = null)
        )
        val otpEntry = record.copy(
            id = otpId,
            type = "OTP",
            summary = record.summary.copy(
                title = "${record.summary.title} OTP",
                tags = emptyList()
            ),
            secret = BackupSecretRecord(otp = otp)
        )
        return BitwardenRecords(
            entries = listOf(account, login, otpEntry),
            links = listOf(
                BackupLinkRecord(
                    id = relatedId(record.id, "member-link"),
                    sourceEntryId = login.id,
                    targetEntryId = account.id,
                    relationType = EntryRelationType.MEMBER_OF_ACCOUNT.name,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                ),
                BackupLinkRecord(
                    id = relatedId(record.id, "otp-link"),
                    sourceEntryId = otpEntry.id,
                    targetEntryId = login.id,
                    relationType = EntryRelationType.OTP_FOR.name,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        )
    }

    private fun parseOtp(value: String): BackupOtpSecret {
        val trimmed = value.trim()
        if (trimmed.startsWith("steam://", ignoreCase = true)) {
            return BackupOtpSecret(
                BackupOtpConfig(
                    type = BackupOtpType.STEAM,
                    secret = trimmed.substringAfter("steam://"),
                    digits = 5
                )
            )
        }
        if (!trimmed.startsWith("otpauth://", ignoreCase = true)) {
            return BackupOtpSecret(BackupOtpConfig(secret = trimmed.filterNot(Char::isWhitespace)))
        }

        val uri = URI(trimmed)
        val query = uri.rawQuery.orEmpty()
            .split('&')
            .filter(String::isNotBlank)
            .associate { part ->
                decode(part.substringBefore('=')) to decode(part.substringAfter('=', ""))
            }
        val label = decode(uri.rawPath.orEmpty().removePrefix("/"))
        val labelParts = label.split(':', limit = 2)
        val type = when (uri.host?.lowercase()) {
            "hotp" -> BackupOtpType.HOTP
            "totp" -> BackupOtpType.TOTP
            else -> throw BackupFailed()
        }
        val secret = query["secret"]?.takeIf(String::isNotBlank)
            ?: throw BackupFailed()
        return BackupOtpSecret(
            BackupOtpConfig(
                type = type,
                secret = secret,
                algorithm = runCatching {
                    BackupOtpAlgorithm.valueOf(query["algorithm"]?.uppercase() ?: "SHA1")
                }.getOrElse { throw BackupFailed() },
                digits = query["digits"]?.toIntOrNull() ?: 6,
                periodSeconds = if (type == BackupOtpType.HOTP) null
                else query["period"]?.toIntOrNull() ?: 30,
                counter = if (type == BackupOtpType.HOTP) {
                    query["counter"]?.toLongOrNull() ?: 0
                } else {
                    null
                },
                issuer = query["issuer"] ?: labelParts.getOrNull(0),
                accountName = labelParts.getOrNull(1)
            )
        )
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())

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

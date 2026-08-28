package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.model.BackupCardCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupIdentityCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLoginCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpAlgorithm
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpConfig
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpEncoding
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupPasskeyCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSecretRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSensitiveField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSshCredential
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSummaryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupWebsiteRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupWifiCredential
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.entry.mergeSensitiveFields
import com.aozijx.passly.data.mapper.entry.toBundleSecret
import com.aozijx.passly.data.mapper.entry.toSensitiveFieldValues
import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.OtpConfigPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.entry.payload.EntryAssociationsPayload
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份文档映射器。
 * 只负责 Entry ↔ BackupEntryRecord 的转换，不涉及加密或文件 IO。
 */
@Singleton
internal class RoomBackupSnapshotMapper @Inject constructor() {

    fun toRecord(
        entry: Entry,
        attachmentIds: List<String> = emptyList()
    ): BackupEntryRecord = BackupEntryRecord(
        id = entry.id.value,
        type = entry.type.name,
        version = entry.version.value,
        createdAt = entry.timestamps.createdAtMs,
        updatedAt = entry.timestamps.updatedAtMs,
        deletedAt = entry.timestamps.deletedAtMs,
        // 本机绝对路径不能进入可移植备份；图标由 BackupResourceRecord 表达。
        summary = EntryProfileMapper.toPayload(entry.profile).toBackupRecord(),
        secret = EntrySecretMapper.toPayload(entry.secret.toBundleSecret()).toBackupRecord(),
        attachmentIds = attachmentIds,
        sensitiveFields = entry.secret.toSensitiveFieldValues().map { (key, value) ->
            BackupSensitiveField(key = key.name, value = value)
        },
    )

    fun toEntry(record: BackupEntryRecord): Entry = Entry(
        identity = EntryIdentity(
            id = EntryId(record.id),
            type = EntryType.valueOf(record.type),
            version = EntryVersion(record.version),
            timestamps = EntryTimestamps(record.createdAt, record.updatedAt, record.deletedAt),
        ),
        profile = EntryProfileMapper.toDomain(record.summary.toPayload()),
        secret = EntrySecretMapper.toDomain(record.secret.toPayload()).mergeSensitiveFields(
            record.sensitiveFields.associate { field ->
                SensitiveFieldKey.valueOf(field.key) to field.value
            },
        ),
    )
}

private fun SummaryPayload.toBackupRecord() = BackupSummaryRecord(
    title = title,
    username = username,
    website = website?.let {
        BackupWebsiteRecord(
            primaryUrl = it.primaryUrl,
            matchDomains = it.matchDomains,
            packageNames = it.packageNames
        )
    },
    icon = icon,
    favorite = favorite,
    tags = tags,
    color = color,
    expiresAt = expiresAt
)

private fun BackupSummaryRecord.toPayload() = SummaryPayload(
    title = title,
    username = username,
    website = website?.let {
        EntryAssociationsPayload(
            primaryUrl = it.primaryUrl,
            matchDomains = it.matchDomains,
            packageNames = it.packageNames
        )
    },
    icon = icon,
    // Machine-local absolute paths are represented by BackupResourceRecord.
    iconCustomPath = null,
    favorite = favorite,
    tags = tags,
    color = color,
    expiresAt = expiresAt
)

private fun SecretPayload.toBackupRecord() = BackupSecretRecord(
    login = login?.let { BackupLoginCredential(it.email, it.password) },
    notes = notes,
    card = card?.let {
        BackupCardCredential(
            cardType = it.cardType,
            cardNumber = it.cardNumber,
            cardExpiry = it.cardExpiry,
            cardCvv = it.cardCvv,
            cardHolder = it.cardHolder,
            paymentPin = it.paymentPin,
            paymentPlatform = it.paymentPlatform,
            billingAddress = it.billingAddress,
        )
    },
    identity = identity?.let {
        BackupIdentityCredential(
            it.idNumber,
            it.securityQuestion,
            it.securityAnswer,
            it.seedPhrase,
            it.recoveryCodes
        )
    },
    ssh = ssh?.let { BackupSshCredential(it.privateKey, it.publicKey, it.passphrase) },
    wifi = wifi?.let { BackupWifiCredential(it.ssid, it.password, it.securityType, it.isHidden) },
    passkey = passkey?.let {
        BackupPasskeyCredential(
            it.credentialId,
            it.rpId,
            it.userHandle,
            it.privateKeyReference,
            it.hardwareKeyInfo
        )
    },
    otp = otp?.let { otpSecret ->
        BackupOtpCredential(
            otpSecret.config?.let {
                BackupOtpConfig(
                    type = BackupOtpType.valueOf(it.type.name),
                    secret = requireNotNull(it.secret) { "OTP secret is missing" },
                    algorithm = BackupOtpAlgorithm.valueOf(it.algorithm.name),
                    digits = it.digits,
                    periodSeconds = it.periodSeconds,
                    counter = it.counter,
                    encoding = BackupOtpEncoding.valueOf(it.encoding.name),
                    issuer = it.issuer,
                    accountName = it.accountName
                )
            }
        )
    },
    customFields = customFields.map { BackupCustomField(it.name, it.value, it.type) }
)

private fun BackupSecretRecord.toPayload() = SecretPayload(
    login = login?.let { LoginCredentialPayload(it.email, it.password) },
    notes = notes,
    card = card?.let {
        CardCredentialPayload(
            cardType = it.cardType,
            cardNumber = it.cardNumber,
            cardExpiry = it.cardExpiry,
            cardCvv = it.cardCvv,
            cardHolder = it.cardHolder,
            paymentPin = it.paymentPin,
            paymentPlatform = it.paymentPlatform,
            billingAddress = it.billingAddress,
        )
    },
    identity = identity?.let {
        IdentityCredentialPayload(
            it.idNumber,
            it.securityQuestion,
            it.securityAnswer,
            it.seedPhrase,
            it.recoveryCodes
        )
    },
    ssh = ssh?.let { SshCredentialPayload(it.privateKey, it.publicKey, it.passphrase) },
    wifi = wifi?.let {
        WifiCredentialPayload(
            ssid = it.ssid,
            password = it.password,
            securityType = it.securityType,
            isHidden = it.hidden,
        )
    },
    passkey = passkey?.let {
        PasskeyCredentialPayload(
            it.credentialId,
            it.rpId,
            it.userHandle,
            it.privateKeyReference,
            it.hardwareKeyInfo
        )
    },
    otp = otp?.let { otpSecret ->
        OtpCredentialPayload(
            otpSecret.config?.let {
                OtpConfigPayload(
                    type = OtpType.valueOf(it.type.name),
                    secret = it.secret,
                    algorithm = OtpHashAlgorithm.valueOf(it.algorithm.name),
                    digits = it.digits,
                    periodSeconds = it.periodSeconds,
                    counter = it.counter,
                    encoding = OtpSecretEncoding.valueOf(it.encoding.name),
                    issuer = it.issuer,
                    accountName = it.accountName
                )
            }
        )
    },
    customFields = customFields.map { CustomFieldPayload(it.name, it.value, it.type) }
)

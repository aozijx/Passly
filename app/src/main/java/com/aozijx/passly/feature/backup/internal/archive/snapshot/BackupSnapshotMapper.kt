package com.aozijx.passly.feature.backup.internal.archive.snapshot

import com.aozijx.passly.feature.backup.internal.archive.model.BackupCardSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomField
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupIdentitySecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLoginSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpAlgorithm
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpConfig
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpEncoding
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupPasskeySecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSecretRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSshSecret
import com.aozijx.passly.feature.backup.internal.archive.model.BackupSummaryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupWebsiteRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupWifiSecret
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.model.payload.secret.CardSecretPayload
import com.aozijx.passly.data.model.payload.secret.CustomFieldPayload
import com.aozijx.passly.data.model.payload.secret.IdentitySecretPayload
import com.aozijx.passly.data.model.payload.secret.LoginSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpConfigPayload
import com.aozijx.passly.data.model.payload.secret.OtpHashAlgorithmPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretEncodingPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpTypePayload
import com.aozijx.passly.data.model.payload.secret.PasskeySecretPayload
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.secret.SshSecretPayload
import com.aozijx.passly.data.model.payload.secret.WifiSecretPayload
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.payload.summary.WebsiteInfoPayload
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份文档映射器。
 * 只负责 EntryAggregate ↔ BackupEntryRecord 的转换，不涉及加密或文件 IO。
 */
@Singleton
internal class BackupSnapshotMapper @Inject constructor() {

    fun toRecord(
        entry: EntryAggregate,
        attachmentIds: List<String> = emptyList()
    ): BackupEntryRecord = BackupEntryRecord(
        id = entry.id,
        type = entry.entryType.name,
        version = entry.entryVersion,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt,
        deletedAt = entry.deletedAt,
        // 本机绝对路径不能进入可移植备份；图标由 BackupResourceRecord 表达。
        summary = EntrySummaryMapper.toPayload(entry.summary).toBackupRecord(),
        secret = EntrySecretMapper.toPayload(entry.secret).toBackupRecord(),
        attachmentIds = attachmentIds
    )

    fun toEntry(record: BackupEntryRecord): EntryAggregate = EntryAggregate(
        header = EntryHeader(
            id = EntryId(record.id),
            entryType = EntryType.valueOf(record.type),
            version = EntryVersion(record.version),
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            deletedAt = record.deletedAt
        ),
        summary = EntrySummaryMapper.toDomain(record.summary.toPayload()),
        secret = EntrySecretMapper.toDomain(record.secret.toPayload())
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
        WebsiteInfoPayload(
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
    login = login?.let { BackupLoginSecret(it.email, it.password) },
    notes = notes,
    card = card?.let {
        BackupCardSecret(
            it.cardNumber,
            it.cardExpiry,
            it.cardCvv,
            it.cardHolder,
            it.paymentPin,
            it.paymentPlatform
        )
    },
    identity = identity?.let {
        BackupIdentitySecret(
            it.idNumber,
            it.securityQuestion,
            it.securityAnswer,
            it.seedPhrase,
            it.recoveryCodes
        )
    },
    ssh = ssh?.let { BackupSshSecret(it.privateKey, it.publicKey, it.passphrase) },
    wifi = wifi?.let { BackupWifiSecret(it.password, it.securityType, it.isHidden) },
    passkey = passkey?.let {
        BackupPasskeySecret(
            it.credentialId,
            it.rpId,
            it.userHandle,
            it.privateKeyReference,
            it.hardwareKeyInfo
        )
    },
    otp = otp?.let { otpSecret ->
        BackupOtpSecret(
            otpSecret.config?.let {
                BackupOtpConfig(
                    type = BackupOtpType.valueOf(it.type.name),
                    secret = it.secret,
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
    login = login?.let { LoginSecretPayload(it.email, it.password) },
    notes = notes,
    card = card?.let {
        CardSecretPayload(
            it.cardNumber,
            it.cardExpiry,
            it.cardCvv,
            it.cardHolder,
            it.paymentPin,
            it.paymentPlatform
        )
    },
    identity = identity?.let {
        IdentitySecretPayload(
            it.idNumber,
            it.securityQuestion,
            it.securityAnswer,
            it.seedPhrase,
            it.recoveryCodes
        )
    },
    ssh = ssh?.let { SshSecretPayload(it.privateKey, it.publicKey, it.passphrase) },
    wifi = wifi?.let { WifiSecretPayload(it.password, it.securityType, it.hidden) },
    passkey = passkey?.let {
        PasskeySecretPayload(
            it.credentialId,
            it.rpId,
            it.userHandle,
            it.privateKeyReference,
            it.hardwareKeyInfo
        )
    },
    otp = otp?.let { otpSecret ->
        OtpSecretPayload(
            otpSecret.config?.let {
                OtpConfigPayload(
                    type = OtpTypePayload.valueOf(it.type.name),
                    secret = it.secret,
                    algorithm = OtpHashAlgorithmPayload.valueOf(it.algorithm.name),
                    digits = it.digits,
                    periodSeconds = it.periodSeconds,
                    counter = it.counter,
                    encoding = OtpSecretEncodingPayload.valueOf(it.encoding.name),
                    issuer = it.issuer,
                    accountName = it.accountName
                )
            }
        )
    },
    customFields = customFields.map { CustomFieldPayload(it.name, it.value, it.type) }
)

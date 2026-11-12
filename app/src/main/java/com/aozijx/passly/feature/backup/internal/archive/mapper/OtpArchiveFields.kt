package com.aozijx.passly.feature.backup.internal.archive.mapper

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue

internal fun OtpConfig.toOtpArchiveFields(): List<BackupFieldRecord> =
    buildList {
        fun put(
            key: FieldKey,
            value: BackupFieldValue,
        ) {
            add(BackupFieldRecord(BackupArchiveKeyRegistry.fieldKey(key), value))
        }

        fun putText(
            key: FieldKey,
            value: String?,
        ) {
            value?.takeUnless(String::isBlank)?.let { put(key, BackupFieldValue.text(it)) }
        }

        val typeKey =
            when (type) {
                OtpType.TOTP -> "totp"
                OtpType.HOTP -> "hotp"
                OtpType.STEAM -> "steam"
            }
        put(FieldKey.OTP_TYPE, BackupFieldValue.text(typeKey))
        putText(FieldKey.OTP_SECRET, secret)
        val (algorithm, digits) =
            when (this@toOtpArchiveFields) {
                is OtpConfig.Totp -> algorithm to digits
                is OtpConfig.Hotp -> algorithm to digits
            }
        put(FieldKey.OTP_ALGORITHM, BackupFieldValue.text(algorithm.archiveKey))
        put(FieldKey.OTP_DIGITS, BackupFieldValue.integer(digits))
        when (this@toOtpArchiveFields) {
            is OtpConfig.Totp -> put(FieldKey.OTP_PERIOD, BackupFieldValue.integer(periodSeconds))
            is OtpConfig.Hotp -> put(FieldKey.OTP_COUNTER, BackupFieldValue.long(counter))
        }
        val encodingKey =
            when (encoding) {
                OtpSecretEncoding.BASE32 -> "base32"
                OtpSecretEncoding.BASE64 -> "base64"
            }
        put(FieldKey.OTP_ENCODING, BackupFieldValue.text(encodingKey))
        putText(FieldKey.OTP_ISSUER, issuer)
        putText(FieldKey.OTP_ACCOUNT_NAME, accountName)
    }

private val OtpHashAlgorithm.archiveKey: String
    get() =
        when (this) {
            OtpHashAlgorithm.SHA1 -> "sha1"
            OtpHashAlgorithm.SHA256 -> "sha256"
            OtpHashAlgorithm.SHA512 -> "sha512"
        }

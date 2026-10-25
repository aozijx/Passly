package com.aozijx.passly.feature.backup.internal.archive.mapper

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue

internal fun OtpConfig.toOtpArchiveFields(): List<BackupFieldRecord> = buildList {
    fun put(key: FieldKey, value: BackupFieldValue) {
        add(BackupFieldRecord(BackupArchiveKeyRegistry.fieldKey(key), value))
    }
    fun putText(key: FieldKey, value: String?) {
        value?.takeUnless(String::isBlank)?.let { put(key, BackupFieldValue.text(it)) }
    }

    val typeKey = when (type) {
        OtpType.TOTP -> "totp"
        OtpType.HOTP -> "hotp"
        OtpType.STEAM -> "steam"
    }
    put(FieldKey.OTP_TYPE, BackupFieldValue.text(typeKey))
    putText(FieldKey.OTP_SECRET, secret)
    val algorithmKey = when (algorithm) {
        OtpHashAlgorithm.SHA1 -> "sha1"
        OtpHashAlgorithm.SHA256 -> "sha256"
        OtpHashAlgorithm.SHA512 -> "sha512"
    }
    put(FieldKey.OTP_ALGORITHM, BackupFieldValue.text(algorithmKey))
    put(FieldKey.OTP_DIGITS, BackupFieldValue.integer(digits))
    periodSeconds?.let { put(FieldKey.OTP_PERIOD, BackupFieldValue.integer(it)) }
    counter?.let { put(FieldKey.OTP_COUNTER, BackupFieldValue.long(it)) }
    val encodingKey = when (encoding) {
        OtpSecretEncoding.BASE32 -> "base32"
        OtpSecretEncoding.BASE64 -> "base64"
    }
    put(FieldKey.OTP_ENCODING, BackupFieldValue.text(encodingKey))
    putText(FieldKey.OTP_ISSUER, issuer)
    putText(FieldKey.OTP_ACCOUNT_NAME, accountName)
}

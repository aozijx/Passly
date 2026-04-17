package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.entity.VaultEntryEntity

internal object BackupFieldEncryptor {
    private const val TAG = "BackupFieldEncryptor"

    /**
     * 将数据库实体转换为可导出的明文实体。
     * 重要：此过程必须在用户已授权（KeyStore 可访问）的情况下进行。
     */
    fun toExportEntry(entry: VaultEntryEntity, iconPathForBackup: String?): VaultEntryEntity {
        fun decryptMandatory(value: String?, fieldName: String): String {
            if (value == null || value.isEmpty()) return ""
            // 这里使用 CryptoAccess.decryptOrNull，如果返回 null 说明解密失败（KeyStore 锁定或损坏）
            val decrypted = CryptoAccess.decryptOrNull(value)
            if (decrypted == null) {
                Logcat.e(
                    TAG,
                    "Field $fieldName decryption FAILED for entry ${entry.id}. Data might be corrupted or KeyStore locked."
                )
                // 如果解密失败，保留原始密文是不安全的，且会导致导入失败。
                // 但在备份场景下，我们不能丢弃数据，此处记录错误。
                return value
            }
            return decrypted
        }

        fun decryptOptional(value: String?): String? {
            if (value == null) return null
            if (value.isEmpty()) return ""
            return CryptoAccess.decryptOrNull(value) ?: value
        }

        return entry.copy(
            username = decryptMandatory(entry.username, "username"),
            password = decryptMandatory(entry.password, "password"),
            notes = decryptOptional(entry.notes),
            totpSecret = decryptOptional(entry.totpSecret),
            passkeyDataJson = decryptOptional(entry.passkeyDataJson),
            recoveryCodes = decryptOptional(entry.recoveryCodes),
            cardCvv = decryptOptional(entry.cardCvv),
            cardExpiration = decryptOptional(entry.cardExpiration),
            idNumber = decryptOptional(entry.idNumber),
            paymentPin = decryptOptional(entry.paymentPin),
            securityAnswer = decryptOptional(entry.securityAnswer),
            sshPrivateKey = decryptOptional(entry.sshPrivateKey),
            cryptoSeedPhrase = decryptOptional(entry.cryptoSeedPhrase),
            customFieldsJson = decryptOptional(entry.customFieldsJson),
            iconCustomPath = iconPathForBackup
        )
    }

    /**
     * 将导出的明文实体重新使用当前设备的 KeyStore 加密并落库。
     */
    fun toImportEntry(entry: VaultEntryEntity, restoredIconPath: String?): VaultEntryEntity {
        fun encryptIfPlain(value: String?): String? {
            if (value == null) return null
            if (value.isEmpty()) return ""
            // 如果已经是加密格式（例如导入了错误的旧版备份），则不再加密
            if (value.startsWith("A:") || value.startsWith("M:")) return value
            return CryptoManager.encrypt(value)
        }

        return entry.copy(
            username = encryptIfPlain(entry.username) ?: "",
            password = encryptIfPlain(entry.password) ?: "",
            notes = encryptIfPlain(entry.notes),
            totpSecret = encryptIfPlain(entry.totpSecret),
            passkeyDataJson = encryptIfPlain(entry.passkeyDataJson),
            recoveryCodes = encryptIfPlain(entry.recoveryCodes),
            cardCvv = encryptIfPlain(entry.cardCvv),
            cardExpiration = encryptIfPlain(entry.cardExpiration),
            idNumber = encryptIfPlain(entry.idNumber),
            paymentPin = encryptIfPlain(entry.paymentPin),
            securityAnswer = encryptIfPlain(entry.securityAnswer),
            sshPrivateKey = encryptIfPlain(entry.sshPrivateKey),
            cryptoSeedPhrase = encryptIfPlain(entry.cryptoSeedPhrase),
            customFieldsJson = encryptIfPlain(entry.customFieldsJson),
            iconCustomPath = restoredIconPath ?: entry.iconCustomPath
        )
    }
}
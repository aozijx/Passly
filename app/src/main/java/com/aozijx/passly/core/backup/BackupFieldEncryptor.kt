package com.aozijx.passly.core.backup

import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.data.entity.VaultEntryEntity

internal object BackupFieldEncryptor {
    fun toExportEntry(entry: VaultEntryEntity, iconPathForBackup: String?): VaultEntryEntity {
        fun decryptIfNotNull(value: String?): String? {
            if (value == null) return null
            if (value.isEmpty()) return ""
            return CryptoAccess.decryptOrNull(value) ?: ""
        }

        return entry.copy(
            username = decryptIfNotNull(entry.username) ?: "",
            password = decryptIfNotNull(entry.password) ?: "",
            notes = decryptIfNotNull(entry.notes),
            totpSecret = decryptIfNotNull(entry.totpSecret),
            passkeyDataJson = decryptIfNotNull(entry.passkeyDataJson),
            recoveryCodes = decryptIfNotNull(entry.recoveryCodes),
            cardCvv = decryptIfNotNull(entry.cardCvv),
            cardExpiration = decryptIfNotNull(entry.cardExpiration),
            idNumber = decryptIfNotNull(entry.idNumber),
            paymentPin = decryptIfNotNull(entry.paymentPin),
            securityAnswer = decryptIfNotNull(entry.securityAnswer),
            sshPrivateKey = decryptIfNotNull(entry.sshPrivateKey),
            cryptoSeedPhrase = decryptIfNotNull(entry.cryptoSeedPhrase),
            customFieldsJson = decryptIfNotNull(entry.customFieldsJson),
            iconCustomPath = iconPathForBackup
        )
    }

    fun toImportEntry(entry: VaultEntryEntity, restoredIconPath: String?): VaultEntryEntity {
        fun encryptIfNotNull(value: String?): String? {
            if (value == null) return null
            if (value.isEmpty()) return ""
            return CryptoManager.encrypt(value)
        }

        return entry.copy(
            username = encryptIfNotNull(entry.username) ?: "",
            password = encryptIfNotNull(entry.password) ?: "",
            notes = encryptIfNotNull(entry.notes),
            totpSecret = encryptIfNotNull(entry.totpSecret),
            passkeyDataJson = encryptIfNotNull(entry.passkeyDataJson),
            recoveryCodes = encryptIfNotNull(entry.recoveryCodes),
            cardCvv = encryptIfNotNull(entry.cardCvv),
            cardExpiration = encryptIfNotNull(entry.cardExpiration),
            idNumber = encryptIfNotNull(entry.idNumber),
            paymentPin = encryptIfNotNull(entry.paymentPin),
            securityAnswer = encryptIfNotNull(entry.securityAnswer),
            sshPrivateKey = encryptIfNotNull(entry.sshPrivateKey),
            cryptoSeedPhrase = encryptIfNotNull(entry.cryptoSeedPhrase),
            customFieldsJson = encryptIfNotNull(entry.customFieldsJson),
            iconCustomPath = restoredIconPath ?: entry.iconCustomPath
        )
    }
}
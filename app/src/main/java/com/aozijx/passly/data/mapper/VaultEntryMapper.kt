package com.aozijx.passly.data.mapper

import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.entity.VaultPayload
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory

fun VaultEntryEntity.toDomain(): VaultEntry {
    val json = CryptoManager.decrypt(encryptedBlob)
    val p = VaultPayload.fromJson(json)
    return VaultEntry(
        id = id,
        title = p.title,
        username = p.username,
        password = p.password,
        email = p.email,
        category = p.category,
        notes = p.notes,
        iconName = p.iconName,
        iconCustomPath = p.iconCustomPath,
        totpSecret = p.totpSecret,
        totpIssuer = p.totpIssuer,
        totpPeriod = p.totpPeriod,
        totpDigits = p.totpDigits,
        totpAlgorithm = p.totpAlgorithm,
        passkeyDataJson = p.passkeyDataJson,
        recoveryCodes = p.recoveryCodes,
        hardwareKeyInfo = p.hardwareKeyInfo,
        wifiSecurityType = p.wifiSecurityType,
        wifiIsHidden = p.wifiIsHidden,
        cardCvv = p.cardCvv,
        cardExpiration = p.cardExpiration,
        idNumber = p.idNumber,
        paymentPin = p.paymentPin,
        paymentPlatform = p.paymentPlatform,
        securityQuestion = p.securityQuestion,
        securityAnswer = p.securityAnswer,
        sshPrivateKey = p.sshPrivateKey,
        cryptoSeedPhrase = p.cryptoSeedPhrase,
        entryType = entryType,
        associatedAppPackage = p.associatedAppPackage,
        associatedDomain = p.associatedDomain,
        uriList = p.uriList,
        matchType = p.matchType,
        customFieldsJson = p.customFieldsJson,
        autoSubmit = p.autoSubmit,
        strengthScore = p.strengthScore,
        lastUsedAt = p.lastUsedAt,
        usageCount = p.usageCount,
        favorite = p.favorite,
        tags = p.tags,
        createdAt = p.createdAt,
        updatedAt = updatedAt,
        expiresAt = p.expiresAt
    )
}

fun VaultEntry.toEntity(): VaultEntryEntity {
    val payload = VaultPayload(
        title = title,
        username = username,
        password = password,
        email = email,
        category = category,
        notes = notes,
        iconName = iconName,
        iconCustomPath = iconCustomPath,
        totpSecret = totpSecret,
        totpIssuer = totpIssuer,
        totpPeriod = totpPeriod,
        totpDigits = totpDigits,
        totpAlgorithm = totpAlgorithm,
        passkeyDataJson = passkeyDataJson,
        recoveryCodes = recoveryCodes,
        hardwareKeyInfo = hardwareKeyInfo,
        wifiSecurityType = wifiSecurityType,
        wifiIsHidden = wifiIsHidden,
        cardCvv = cardCvv,
        cardExpiration = cardExpiration,
        idNumber = idNumber,
        paymentPin = paymentPin,
        paymentPlatform = paymentPlatform,
        securityQuestion = securityQuestion,
        securityAnswer = securityAnswer,
        sshPrivateKey = sshPrivateKey,
        cryptoSeedPhrase = cryptoSeedPhrase,
        entryType = entryType,
        associatedAppPackage = associatedAppPackage,
        associatedDomain = associatedDomain,
        uriList = uriList,
        matchType = matchType,
        customFieldsJson = customFieldsJson,
        autoSubmit = autoSubmit,
        strengthScore = strengthScore,
        lastUsedAt = lastUsedAt,
        usageCount = usageCount,
        favorite = favorite,
        tags = tags,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
    return VaultEntryEntity(
        id = id,
        entryType = entryType,
        encryptedBlob = CryptoManager.encrypt(payload.toJson()),
        updatedAt = updatedAt ?: System.currentTimeMillis()
    )
}

fun VaultHistoryEntity.toDomain(): VaultHistory = VaultHistory(
    historyId = historyId,
    entryId = entryId,
    fieldName = fieldName,
    oldValue = oldValue,
    newValue = newValue,
    changeType = changeType,
    deviceName = deviceName,
    changedAt = changedAt
)

fun VaultHistory.toEntity(): VaultHistoryEntity = VaultHistoryEntity(
    historyId = historyId,
    entryId = entryId,
    fieldName = fieldName,
    oldValue = oldValue,
    newValue = newValue,
    changeType = changeType,
    deviceName = deviceName,
    changedAt = changedAt
)

fun List<VaultEntryEntity>.toDomainList(): List<VaultEntry> = map { it.toDomain() }
fun List<VaultHistoryEntity>.toDomainHistoryList(): List<VaultHistory> = map { it.toDomain() }

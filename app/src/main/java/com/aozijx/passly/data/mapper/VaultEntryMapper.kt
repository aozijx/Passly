package com.aozijx.passly.data.mapper

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory

fun VaultEntryEntity.toDomain(): VaultEntry = VaultEntry(
    id = id,
    title = title,
    username = username,
    password = password,
    category = category,
    notes = notes,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    totpSecret = totpSecret,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    passkeyDataJson = passkeyDataJson,
    recoveryCodes = recoveryCodes,
    hardwareKeyInfo = hardwareKeyInfo,
    wifiEncryptionType = wifiEncryptionType,
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
    encryptedImageData = encryptedImageData,
    strengthScore = strengthScore,
    lastUsedAt = lastUsedAt,
    usageCount = usageCount,
    favorite = favorite,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expiresAt = expiresAt
)

fun VaultEntry.toEntity(): VaultEntryEntity = VaultEntryEntity(
    id = id,
    title = title,
    username = username,
    password = password,
    category = category,
    notes = notes,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    totpSecret = totpSecret,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    passkeyDataJson = passkeyDataJson,
    recoveryCodes = recoveryCodes,
    hardwareKeyInfo = hardwareKeyInfo,
    wifiEncryptionType = wifiEncryptionType,
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
    encryptedImageData = encryptedImageData,
    strengthScore = strengthScore,
    lastUsedAt = lastUsedAt,
    usageCount = usageCount,
    favorite = favorite,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expiresAt = expiresAt
)

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

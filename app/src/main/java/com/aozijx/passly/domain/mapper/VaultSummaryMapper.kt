package com.aozijx.passly.domain.mapper

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary

fun VaultEntry.toSummary(): VaultSummary = VaultSummary(
    id = id,
    title = title,
    category = category,
    entryType = entryType,
    username = username,
    email = email,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    associatedAppPackage = associatedAppPackage,
    associatedDomain = associatedDomain,
    totpSecret = totpSecret,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    favorite = favorite,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

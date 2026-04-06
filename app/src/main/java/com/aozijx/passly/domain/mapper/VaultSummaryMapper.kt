package com.aozijx.passly.domain.mapper

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary

fun VaultEntry.toSummary(): VaultSummary = VaultSummary(
    id = id,
    title = title,
    category = category,
    entryType = entryType,
    username = username,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    associatedAppPackage = associatedAppPackage,
    associatedDomain = associatedDomain,
    totpSecret = totpSecret,
    totpPeriod = totpPeriod,
    totpDigits = totpDigits,
    totpAlgorithm = totpAlgorithm,
    favorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)

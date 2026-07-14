package com.aozijx.passly.data.mapper.metadata

import com.aozijx.passly.data.model.payload.metadata.MetadataPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toMetadataPayload(): MetadataPayload = MetadataPayload(
    title = title,
    category = category,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    associatedAppPackage = associatedAppPackage,
    associatedDomain = associatedDomain,
    uriList = uriList,
    matchType = matchType,
    autoSubmit = autoSubmit,
    favorite = favorite,
    tags = tags,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expiresAt = expiresAt
)

fun VaultEntry.mergeMetadata(payload: MetadataPayload): VaultEntry = copy(
    title = payload.title,
    category = payload.category,
    iconName = payload.iconName,
    iconCustomPath = payload.iconCustomPath,
    associatedAppPackage = payload.associatedAppPackage,
    associatedDomain = payload.associatedDomain,
    uriList = payload.uriList,
    matchType = payload.matchType,
    autoSubmit = payload.autoSubmit,
    favorite = payload.favorite,
    tags = payload.tags,
    lastUsedAt = payload.lastUsedAt,
    updatedAt = payload.updatedAt,
    expiresAt = payload.expiresAt
)

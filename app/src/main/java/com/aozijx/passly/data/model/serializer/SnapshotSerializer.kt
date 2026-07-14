package com.aozijx.passly.data.model.serializer

import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import kotlinx.serialization.builtins.ListSerializer

fun VaultSnapshot.toJsonString(): String =
    AppJson.encodeToString(VaultSnapshot.serializer(), this)

fun String.toVaultSnapshot(): VaultSnapshot =
    AppJson.decodeFromString(VaultSnapshot.serializer(), this)

fun List<VaultSnapshot>.toJsonString(): String =
    AppJson.encodeToString(ListSerializer(VaultSnapshot.serializer()), this)

fun String.toVaultSnapshotList(): List<VaultSnapshot> =
    AppJson.decodeFromString(ListSerializer(VaultSnapshot.serializer()), this)

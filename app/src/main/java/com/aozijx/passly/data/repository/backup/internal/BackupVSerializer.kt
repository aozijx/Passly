package com.aozijx.passly.data.repository.backup.internal

import kotlinx.serialization.builtins.ListSerializer
import java.io.InputStream
import java.io.OutputStream

internal object BackupVSerializer {

    fun writeEntries(output: OutputStream, payloads: List<VaultPayload>) {
        val json = VaultJson.encodeToString(ListSerializer(VaultPayload.serializer()), payloads)
        output.write(json.toByteArray(Charsets.UTF_8))
    }

    fun readEntries(input: InputStream): List<VaultPayload> {
        val json = input.readBytes().toString(Charsets.UTF_8)
        return VaultJson.decodeFromString(ListSerializer(VaultPayload.serializer()), json)
    }
}

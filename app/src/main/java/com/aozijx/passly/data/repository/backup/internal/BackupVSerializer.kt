package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.data.model.payload.backup.VaultSnapshot
import com.aozijx.passly.data.serializer.PayloadSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.InputStream
import java.io.OutputStream

internal object BackupVSerializer {

    fun writeEntries(output: OutputStream, payloads: List<VaultSnapshot>) {
        output.write(
            PayloadSerializer.serialize(
                payloads,
                ListSerializer(VaultSnapshot.serializer())
            )
        )
    }

    fun readEntries(input: InputStream): List<VaultSnapshot> =
        PayloadSerializer.deserialize(input.readBytes(), ListSerializer(VaultSnapshot.serializer()))
}
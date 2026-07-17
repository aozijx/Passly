package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object BootstrapSerializer : Serializer<BootstrapData> {

    override val defaultValue: BootstrapData
        get() = BootstrapData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): BootstrapData {
        return try {
            BootstrapData.parseFrom(input)
        } catch (error: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read vault bootstrap data.", error)
        }
    }

    override suspend fun writeTo(t: BootstrapData, output: OutputStream) {
        t.writeTo(output)
    }
}

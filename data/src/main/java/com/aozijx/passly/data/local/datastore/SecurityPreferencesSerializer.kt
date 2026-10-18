package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.aozijx.passly.data.preferences.proto.SecurityPreferences
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SecurityPreferencesSerializer : Serializer<SecurityPreferences> {
    override val defaultValue: SecurityPreferences = SecurityPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SecurityPreferences =
        try {
            SecurityPreferences.parseFrom(input)
        } catch (error: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read security preferences.", error)
        }

    override suspend fun writeTo(t: SecurityPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.aozijx.passly.data.local.datastore.diagnostics.DiagnosticsSettings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object DiagnosticsSettingsSerializer : Serializer<DiagnosticsSettings> {
    override val defaultValue: DiagnosticsSettings = DiagnosticsSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DiagnosticsSettings = try {
        DiagnosticsSettings.parseFrom(input)
    } catch (error: InvalidProtocolBufferException) {
        throw CorruptionException("Unable to read diagnostics settings.", error)
    }

    override suspend fun writeTo(t: DiagnosticsSettings, output: OutputStream) = t.writeTo(output)
}

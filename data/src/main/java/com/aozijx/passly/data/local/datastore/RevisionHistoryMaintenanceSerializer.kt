package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.aozijx.passly.data.maintenance.proto.RevisionHistoryMaintenance
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

internal object RevisionHistoryMaintenanceSerializer : Serializer<RevisionHistoryMaintenance> {
    override val defaultValue: RevisionHistoryMaintenance
        get() = RevisionHistoryMaintenance.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): RevisionHistoryMaintenance = try {
        RevisionHistoryMaintenance.parseFrom(input)
    } catch (error: InvalidProtocolBufferException) {
        throw CorruptionException("Unable to read revision history maintenance state.", error)
    }

    override suspend fun writeTo(t: RevisionHistoryMaintenance, output: OutputStream) {
        t.writeTo(output)
    }
}

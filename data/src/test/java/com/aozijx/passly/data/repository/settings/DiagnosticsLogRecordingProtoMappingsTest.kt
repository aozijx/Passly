package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.DiagnosticsLogRecordingPreference
import com.aozijx.passly.data.preferences.proto.DiagnosticsPreferences
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsLogRecordingProtoMappingsTest {
    @Test
    fun `every domain mode has a stable proto mapping`() {
        DiagnosticsLogRecordingMode.entries.forEach { mode ->
            assertEquals(mode, mode.toProto().toDomain())
        }
    }

    @Test
    fun `missing and unspecified preference default to info and above`() {
        assertEquals(
            DiagnosticsLogRecordingMode.INFO_AND_ABOVE,
            readDiagnosticsLogRecordingMode(DiagnosticsPreferences.getDefaultInstance()),
        )
        assertEquals(
            DiagnosticsLogRecordingMode.INFO_AND_ABOVE,
            DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_UNSPECIFIED.toDomain(),
        )
        assertEquals(
            DiagnosticsLogRecordingMode.INFO_AND_ABOVE,
            readDiagnosticsLogRecordingMode(
                DiagnosticsPreferences.parseFrom(byteArrayOf(0x08, 0x05)),
            ),
        )
    }
}

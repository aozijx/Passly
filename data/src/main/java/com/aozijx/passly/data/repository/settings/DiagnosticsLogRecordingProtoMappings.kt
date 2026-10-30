package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.DiagnosticsLogRecordingPreference
import com.aozijx.passly.data.preferences.proto.DiagnosticsPreferences
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode

internal fun DiagnosticsLogRecordingMode.toProto(): DiagnosticsLogRecordingPreference = when (this) {
    DiagnosticsLogRecordingMode.DISABLED ->
        DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_DISABLED
    DiagnosticsLogRecordingMode.WARN_AND_ABOVE ->
        DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_WARN_AND_ABOVE
    DiagnosticsLogRecordingMode.INFO_AND_ABOVE ->
        DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_INFO_AND_ABOVE
    DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE ->
        DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_DEBUG_AND_ABOVE
}

internal fun DiagnosticsLogRecordingPreference.toDomain(): DiagnosticsLogRecordingMode = when (this) {
    DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_DISABLED ->
        DiagnosticsLogRecordingMode.DISABLED
    DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_WARN_AND_ABOVE ->
        DiagnosticsLogRecordingMode.WARN_AND_ABOVE
    DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_INFO_AND_ABOVE ->
        DiagnosticsLogRecordingMode.INFO_AND_ABOVE
    DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_DEBUG_AND_ABOVE ->
        DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE
    DiagnosticsLogRecordingPreference.DIAGNOSTICS_LOG_RECORDING_UNSPECIFIED,
        -> DiagnosticsLogRecordingMode.DEFAULT
}

internal fun readDiagnosticsLogRecordingMode(proto: DiagnosticsPreferences): DiagnosticsLogRecordingMode =
    proto.logRecordingMode.toDomain()

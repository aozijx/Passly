package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import kotlinx.coroutines.flow.Flow

interface DiagnosticsLogSettingsSource {
    val diagnosticsLogRecordingMode: Flow<DiagnosticsLogRecordingMode>
}

interface DiagnosticsLogSettingsRepository : DiagnosticsLogSettingsSource {
    suspend fun setDiagnosticsLogRecordingMode(mode: DiagnosticsLogRecordingMode)
}

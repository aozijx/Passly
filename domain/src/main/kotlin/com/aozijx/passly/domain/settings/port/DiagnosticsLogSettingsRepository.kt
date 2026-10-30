package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import kotlinx.coroutines.flow.Flow

interface DiagnosticsLogSettingsRepository {
    val diagnosticsLogRecordingMode: Flow<DiagnosticsLogRecordingMode>

    suspend fun setDiagnosticsLogRecordingMode(mode: DiagnosticsLogRecordingMode)
}

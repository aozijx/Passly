package com.aozijx.passly.feature.settings.presentation.mapping

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode

@StringRes
internal fun DiagnosticsLogRecordingMode.labelResource(): Int =
    when (this) {
        DiagnosticsLogRecordingMode.DISABLED -> R.string.settings_diagnostics_recording_disabled
        DiagnosticsLogRecordingMode.WARN_AND_ABOVE -> R.string.settings_diagnostics_recording_warn_and_above
        DiagnosticsLogRecordingMode.INFO_AND_ABOVE -> R.string.settings_diagnostics_recording_info_and_above
        DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE -> R.string.all
    }

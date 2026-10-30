package com.aozijx.passly.domain.settings.model

enum class DiagnosticsLogRecordingMode {
    DISABLED,
    WARN_AND_ABOVE,
    INFO_AND_ABOVE,
    DEBUG_AND_ABOVE,
    ;

    companion object {
        val DEFAULT = INFO_AND_ABOVE
    }
}

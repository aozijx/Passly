package com.aozijx.passly.feature.settings.general

sealed interface DiagnosticsSettingsEffect {
    data object ExportFailed : DiagnosticsSettingsEffect
}

package com.aozijx.passly.core.diagnostics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.local.datastore.DiagnosticsSettingsSerializer
import com.aozijx.passly.data.local.datastore.diagnostics.DiagnosticsSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.diagnosticsDataStore: DataStore<DiagnosticsSettings> by dataStore(
    fileName = "diagnostics_settings.pb",
    serializer = DiagnosticsSettingsSerializer
)

class DiagnosticsPolicyStore(private val context: Context) {
    val settings: Flow<DiagnosticsSettings> = context.diagnosticsDataStore.data

    val fileLoggingEnabled: Flow<Boolean> = settings.map { settings ->
        settings.fileLoggingEnabledUntilMs > System.currentTimeMillis()
    }

    suspend fun enableFileLogging(durationMs: Long = DIAGNOSTIC_WINDOW_MS) {
        val until = System.currentTimeMillis() + durationMs
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setFileLoggingEnabledUntilMs(until).build()
        }
    }

    suspend fun disableFileLogging() {
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setFileLoggingEnabledUntilMs(0L).build()
        }
    }

    suspend fun setAndroidSinkEnabled(enabled: Boolean) {
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setAndroidSinkEnabled(enabled).build()
        }
    }

    companion object {
        const val DIAGNOSTIC_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}

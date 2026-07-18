package com.aozijx.passly.data.local.datastore.diagnostics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.core.diagnostics.DiagnosticsPolicy
import com.aozijx.passly.core.diagnostics.DiagnosticsPolicyController
import com.aozijx.passly.data.local.datastore.DiagnosticsSettingsSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.diagnosticsDataStore: DataStore<DiagnosticsSettings> by dataStore(
    fileName = "diagnostics_settings.pb",
    serializer = DiagnosticsSettingsSerializer
)

@Singleton
class ProtoDiagnosticsPolicyController @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DiagnosticsPolicyController {
    override val policies: Flow<DiagnosticsPolicy> = context.diagnosticsDataStore.data.map {
        DiagnosticsPolicy(
            androidSinkEnabled = it.androidSinkEnabled,
            fileLoggingEnabledUntilMs = it.fileLoggingEnabledUntilMs
        )
    }

    override suspend fun enableFileLogging(durationMs: Long) {
        val until = System.currentTimeMillis() + durationMs
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setFileLoggingEnabledUntilMs(until).build()
        }
    }

    override suspend fun disableFileLogging() {
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setFileLoggingEnabledUntilMs(0L).build()
        }
    }

    override suspend fun setAndroidSinkEnabled(enabled: Boolean) {
        context.diagnosticsDataStore.updateData {
            it.toBuilder().setAndroidSinkEnabled(enabled).build()
        }
    }
}

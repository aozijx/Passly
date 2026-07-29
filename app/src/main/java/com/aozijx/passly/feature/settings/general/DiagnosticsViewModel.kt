package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.DiagnosticsExportService
import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.aozijx.passly.app.diagnostics.AppTelemetry

sealed interface DiagnosticsEvent {
    data object ExportFailed : DiagnosticsEvent
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val policies: TelemetryPolicyController,
    private val authenticationManager: AuthenticationManager,
    private val runtime: DiagnosticsRuntimeController,
    private val exportService: DiagnosticsExportService
) : ViewModel() {
    private val eventChannel = Channel<DiagnosticsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val fileLoggingEnabled: StateFlow<Boolean> = policies.policies
        .map { it.isEncryptedFileEnabled() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    fun setFileLoggingEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) policies.enableEncryptedFile() else policies.disableEncryptedFile()
    }

    suspend fun readPage(): String = withContext(Dispatchers.IO) {
        runtime.readLines(MAX_VIEW_LINES).joinToString("\n")
    }

    fun clear() = viewModelScope.launch(Dispatchers.IO) {
        runtime.clear()
    }

    fun authenticateAndExport() = viewModelScope.launch {
        val result = authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.EXPORT_DIAGNOSTICS)
        )
        if (result !is AuthenticationResult.Success) return@launch
        runCatching {
            withContext(Dispatchers.IO) {
                exportService.createPlaintextExport()
            }
        }.mapCatching { file ->
            exportService.share(file).getOrThrow()
        }.onFailure { error ->
            AppTelemetry.e("DiagnosticsExport", "Plaintext diagnostics export failed", error)
            eventChannel.trySend(DiagnosticsEvent.ExportFailed)
        }
    }

    private companion object {
        const val MAX_VIEW_LINES = 500
    }
}

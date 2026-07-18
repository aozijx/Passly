package com.aozijx.passly.feature.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.diagnostics.DiagnosticsExportManager
import com.aozijx.passly.core.diagnostics.DiagnosticsPolicyController
import com.aozijx.passly.core.diagnostics.DiagnosticsRuntime
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val policies: DiagnosticsPolicyController,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {
    val fileLoggingEnabled: StateFlow<Boolean> = policies.policies
        .map { BuildConfig.DEBUG || it.isFileLoggingEnabled() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    fun setFileLoggingEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) policies.enableFileLogging() else policies.disableFileLogging()
    }

    suspend fun readPage(): String = withContext(Dispatchers.IO) {
        DiagnosticsRuntime.readAll().take(MAX_VIEW_LINES).joinToString("\n")
    }

    fun clear() = viewModelScope.launch(Dispatchers.IO) {
        DiagnosticsRuntime.clear()
    }

    fun authenticateAndExport(context: Context) = viewModelScope.launch {
        val result = authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.EXPORT_DIAGNOSTICS)
        )
        if (result !is AuthenticationResult.Success) return@launch
        val file = withContext(Dispatchers.IO) {
            DiagnosticsExportManager.createPlaintextExport(context.applicationContext)
        }
        DiagnosticsExportManager.share(context.applicationContext, file)
    }

    private companion object {
        const val MAX_VIEW_LINES = 500
    }
}

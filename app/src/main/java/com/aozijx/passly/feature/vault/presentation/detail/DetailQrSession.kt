package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.detail.EntryQrExportResult
import com.aozijx.passly.feature.vault.detail.EntryQrKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DetailQrSession(
    private val scope: CoroutineScope,
    private val access: SecureSessionAccessState,
    private val export: suspend (Entry) -> EntryQrExportResult,
) {
    private val mutableState = MutableStateFlow<DetailQrState>(DetailQrState.Hidden)
    val state = mutableState.asStateFlow()
    private var requestJob: Job? = null
    private var generation = 0L

    init {
        scope.launch {
            try {
                access.authenticationState.collect {
                    if (!access.hasFullSecureSessionAccess()) close()
                }
            } finally {
                close()
            }
        }
    }

    fun request(entry: Entry) {
        close()
        if (!scope.isActive || !access.hasFullSecureSessionAccess()) return
        val kind = when (entry.type) {
            EntryType.OTP -> EntryQrKind.Otp
            EntryType.WIFI -> EntryQrKind.Wifi
            else -> return
        }
        val currentGeneration = generation
        mutableState.value = DetailQrState.Loading(kind)
        requestJob = scope.launch {
            val result = try {
                export(entry)
            } catch (cancelled: CancellationException) {
                if (currentGeneration == generation) mutableState.value = DetailQrState.Hidden
                throw cancelled
            }
            if (!isActive || currentGeneration != generation || !access.hasFullSecureSessionAccess()) {
                (result as? EntryQrExportResult.Ready)?.content?.wipe()
                return@launch
            }
            mutableState.value = when (result) {
                is EntryQrExportResult.Ready -> {
                    DetailQrState.Ready(result.kind, DetailQrPayload(result.content))
                }
                EntryQrExportResult.Unavailable -> DetailQrState.Failed(kind)
                EntryQrExportResult.Denied,
                EntryQrExportResult.Cancelled -> DetailQrState.Hidden
            }
        }
    }

    fun close() {
        generation++
        requestJob?.cancel()
        requestJob = null
        try {
            (mutableState.value as? DetailQrState.Ready)?.content?.close()
        } finally {
            mutableState.value = DetailQrState.Hidden
        }
    }
}

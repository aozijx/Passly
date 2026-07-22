package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.feature.vault.model.TotpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class TotpCoordinator(
    private val scope: CoroutineScope,
    private val codeGenerator: (OtpConfig) -> String,
    private val loadEntryById: suspend (String) -> VaultEntry?
) {
    private val _states = MutableStateFlow<Map<String, TotpState>>(emptyMap())
    val states: StateFlow<Map<String, TotpState>> = _states

    fun start(entriesProvider: () -> List<VaultListItem>) {
        scope.launch {
            refresherFlow(entriesProvider).collect { refreshed ->
                _states.value = refreshed
            }
        }
    }

    private fun refresherFlow(
        entriesProvider: () -> List<VaultListItem>,
        intervalMs: Long = 500L
    ): Flow<Map<String, TotpState>> = flow {
        var lastEmitted: Map<String, TotpState>? = null
        while (currentCoroutineContext().isActive) {
            val currentStates = _states.value
            if (currentStates.isNotEmpty()) {
                val refreshed = refreshStates(currentStates, entriesProvider())
                if (refreshed != lastEmitted) {
                    lastEmitted = refreshed
                    emit(refreshed)
                }
            }
            delay(intervalMs)
        }
    }

    private fun refreshStates(
        current: Map<String, TotpState>,
        items: List<VaultListItem>,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): Map<String, TotpState> {
        if (current.isEmpty()) return current
        return current.mapValues { (id, state) ->
            val item = items.find { it.id == id } ?: return@mapValues state
            val secret = state.decryptedSecret ?: return@mapValues state
            val period = item.totpPeriod.coerceAtLeast(1)
            val remaining = period - (nowSeconds % period)
            val code = codeGenerator(
                OtpConfig(
                    secret = secret,
                    digits = item.totpDigits,
                    period = item.totpPeriod,
                    algorithm = item.totpAlgorithm,
                    issuer = item.category,
                    label = item.title
                )
            )
            state.copy(code = code, progress = remaining.toFloat() / period)
        }
    }

    fun unlock(entryId: String, decryptedSecret: String) {
        _states.update { it + (entryId to TotpState("------", 1f, decryptedSecret)) }
    }

    fun autoUnlock(entryId: String) {
        if (_states.value.containsKey(entryId)) return
        scope.launch {
            val entry = loadEntryById(entryId) ?: return@launch
            val secret = entry.credential.otp?.secret
            if (secret.isNullOrBlank()) {
                AppLog.w("TotpCoordinator", "Auto unlock failed: no TOTP secret for $entryId")
                return@launch
            }
            unlock(entryId, secret)
        }
    }

    fun clearSensitiveState(entryId: String) {
        _states.update { it - entryId }
    }

    fun onEntryUpdated(entryId: String) {
        clearSensitiveState(entryId)
        autoUnlock(entryId)
    }
}

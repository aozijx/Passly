package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.TotpState
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
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
    private val codeGenerator: (TotpConfig) -> String,
    private val decryptSecret: (String?) -> String?
) {
    private val _states = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val states: StateFlow<Map<Int, TotpState>> = _states

    fun start(entriesProvider: () -> List<VaultSummary>) {
        scope.launch {
            refresherFlow(entriesProvider).collect { refreshed ->
                _states.value = refreshed
            }
        }
    }

    private fun refresherFlow(
        entriesProvider: () -> List<VaultSummary>,
        intervalMs: Long = 500L
    ): Flow<Map<Int, TotpState>> = flow {
        var lastEmitted: Map<Int, TotpState>? = null
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
        current: Map<Int, TotpState>,
        entries: List<VaultSummary>,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): Map<Int, TotpState> {
        if (current.isEmpty()) return current
        return current.mapValues { (id, state) ->
            val entry = entries.find { it.id == id } ?: return@mapValues state
            val secret = state.decryptedSecret ?: return@mapValues state
            val period = entry.totpPeriod.coerceAtLeast(1)
            val remaining = period - (nowSeconds % period)
            val code = codeGenerator(
                TotpConfig(
                    secret = secret,
                    digits = entry.totpDigits,
                    period = entry.totpPeriod,
                    algorithm = entry.totpAlgorithm,
                    issuer = entry.category,
                    label = entry.title
                )
            )
            state.copy(code = code, progress = remaining.toFloat() / period)
        }
    }

    fun unlock(entryId: Int, decryptedSecret: String) {
        _states.update { it + (entryId to TotpState("------", 1f, decryptedSecret)) }
    }

    fun autoUnlock(entry: VaultSummary) {
        if (_states.value.containsKey(entry.id)) return
        val decrypted = decryptSecret(entry.totpSecret)
        if (decrypted == null) {
            Logcat.w("TotpCoordinator", "Auto unlock failed: secret decrypt returned null")
            return
        }
        unlock(entry.id, decrypted)
    }

    fun autoUnlock(entry: VaultEntry) = autoUnlock(entry.toSummary())

    fun clearSensitiveState(entryId: Int) {
        _states.update { it - entryId }
    }

    fun onEntryUpdated(entry: VaultEntry) {
        clearSensitiveState(entry.id)
        if (!entry.totpSecret.isNullOrBlank()) {
            autoUnlock(entry)
        }
    }
}
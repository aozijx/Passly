package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.presentation.VaultSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class TotpCoordinator(
    private val scope: CoroutineScope,
    private val codeGenerator: (TotpConfig) -> String,
    private val decryptSecret: (String?) -> String?
) {
    private val helper = TotpHelper()
    private val _states = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val states: StateFlow<Map<Int, TotpState>> = _states

    fun start(entriesProvider: () -> List<VaultSummary>) {
        scope.launch {
            helper.runRefresher(
                statesProvider = { _states.value },
                entriesProvider = entriesProvider,
                updateStates = { _states.value = it },
                codeGenerator = codeGenerator
            )
        }
    }

    fun unlock(entryId: Int, decryptedSecret: String) {
        _states.update { helper.unlock(it, entryId, decryptedSecret) }
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

    fun removeEntry(entryId: Int) {
        _states.update { it - entryId }
    }
}

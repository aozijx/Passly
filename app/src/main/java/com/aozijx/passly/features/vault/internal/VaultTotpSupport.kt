package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.core.designsystem.state.TotpState
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.model.presentation.VaultSummary
import kotlinx.coroutines.delay

internal class VaultTotpSupport {

    suspend fun runRefresher(
        statesProvider: () -> Map<Int, TotpState>,
        entriesProvider: () -> List<VaultSummary>,
        updateStates: (Map<Int, TotpState>) -> Unit,
        codeGenerator: (TotpConfig) -> String,
        intervalMs: Long = 500L
    ) {
        while (true) {
            val current = statesProvider()
            if (current.isNotEmpty()) {
                val refreshed = refreshStates(
                    current = current,
                    entries = entriesProvider(),
                    codeGenerator = codeGenerator
                )
                updateStates(refreshed)
            }
            delay(intervalMs)
        }
    }

    fun refreshStates(
        current: Map<Int, TotpState>,
        entries: List<VaultSummary>,
        codeGenerator: (TotpConfig) -> String,
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

    fun unlock(
        current: Map<Int, TotpState>,
        entryId: Int,
        decryptedSecret: String
    ): Map<Int, TotpState> {
        return current + (entryId to TotpState("------", 1f, decryptedSecret))
    }
}

package com.aozijx.passly.feature.detail.page.internal

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyProvider

internal data class DetailEntryAnalysis(
    val vaultType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean
)

internal class DetailEntryAnalyzer(
    private val strategyProvider: EntryTypeStrategyProvider
) {
    fun analyze(entry: VaultEntry): DetailEntryAnalysis {
        val vaultType = entry.entryType
        val strategy = runCatching { strategyProvider.getStrategy(vaultType) }.getOrNull()
        val validationError =
            strategy?.let { it.validateRequiredFields(entry) ?: it.validateFieldContent(entry) }
        val summary = strategy?.extractSummary(entry).orEmpty()

        return DetailEntryAnalysis(
            vaultType = vaultType,
            strategySummary = summary,
            validationError = validationError,
            strategyReady = strategy != null
        )
    }
}

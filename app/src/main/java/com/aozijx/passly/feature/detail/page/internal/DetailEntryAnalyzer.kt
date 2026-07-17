package com.aozijx.passly.feature.detail.page.internal

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory

internal data class DetailEntryAnalysis(
    val vaultType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean
)

internal class DetailEntryAnalyzer {
    fun analyze(entry: VaultEntry): DetailEntryAnalysis {
        val vaultType = entry.entryType
        val strategy = runCatching { EntryTypeStrategyFactory.getStrategy(vaultType) }.getOrNull()
        val validationError = strategy?.validateRequiredFields(entry) ?: strategy?.validateFieldContent(entry)
        val summary = strategy?.extractSummary(entry).orEmpty()

        return DetailEntryAnalysis(
            vaultType = vaultType,
            strategySummary = summary,
            validationError = validationError,
            strategyReady = strategy != null
        )
    }
}
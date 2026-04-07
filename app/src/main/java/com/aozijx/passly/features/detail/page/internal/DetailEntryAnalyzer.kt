package com.aozijx.passly.features.detail.page.internal

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory

internal data class DetailEntryAnalysis(
    val vaultType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean
)

internal class DetailEntryAnalyzer {
    fun analyze(entry: VaultEntry): DetailEntryAnalysis {
        val vaultType = EntryType.fromValue(entry.entryType)
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

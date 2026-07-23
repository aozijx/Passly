package com.aozijx.passly.feature.detail.page.internal

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.service.entry.EntryTypePolicy
import com.aozijx.passly.domain.service.entry.EntryValidatorProvider

internal data class DetailEntryAnalysis(
    val vaultType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean
)

internal class DetailEntryAnalyzer(
    private val entryTypePolicy: EntryTypePolicy,
    private val entryValidatorProvider: EntryValidatorProvider
) {
    fun analyze(entry: VaultEntry): DetailEntryAnalysis {
        val vaultType = entry.entryType
        val validator = entryValidatorProvider.getValidator(vaultType)
        val validationError =
            validator.validateRequiredFields(entry) ?: validator.validateFieldContent(entry)
        val summary = entryTypePolicy.extractSummary(vaultType, entry)

        return DetailEntryAnalysis(
            vaultType = vaultType,
            strategySummary = summary,
            validationError = validationError,
            strategyReady = true
        )
    }
}

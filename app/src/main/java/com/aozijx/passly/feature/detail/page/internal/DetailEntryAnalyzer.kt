package com.aozijx.passly.feature.detail.page.internal

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.service.EntryTypePolicy
import com.aozijx.passly.domain.entry.service.EntryValidatorProvider

internal data class DetailEntryAnalysis(
    val entryType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean
)

internal class DetailEntryAnalyzer(
    private val entryTypePolicy: EntryTypePolicy,
    private val entryValidatorProvider: EntryValidatorProvider
) {
    fun analyze(entry: EntryAggregate): DetailEntryAnalysis {
        val entryType = entry.entryType
        val validator = entryValidatorProvider.getValidator(entryType)
        val validationError =
            validator.validateRequiredFields(entry) ?: validator.validateFieldContent(entry)
        val summary = entryTypePolicy.extractSummary(entryType, entry)

        return DetailEntryAnalysis(
            entryType = entryType,
            strategySummary = summary,
            validationError = validationError,
            strategyReady = true
        )
    }
}

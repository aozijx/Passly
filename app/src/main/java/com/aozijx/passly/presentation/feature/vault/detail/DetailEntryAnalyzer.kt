package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryValidation
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver

internal data class DetailEntryAnalysis(
    val entryType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean,
    val sections: List<DetailSectionKey>,
)

internal class DetailEntryAnalyzer(
    private val entryTypePolicy: EntryTypePolicy,
) {
    fun analyze(entry: Entry): DetailEntryAnalysis {
        val entryType = entry.type
        val validationError = EntryValidation.validate(entry).firstOrNull()?.let {
            "${it.field.name}:${it.code.name}"
        }
        val summary = entryTypePolicy.extractSummary(entryType, entry)

        return DetailEntryAnalysis(
            entryType = entryType,
            strategySummary = summary,
            validationError = validationError,
            strategyReady = true,
            sections = DetailSectionResolver.resolve(entry),
        )
    }
}

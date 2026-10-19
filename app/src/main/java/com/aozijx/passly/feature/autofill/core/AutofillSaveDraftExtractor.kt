package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.feature.autofill.internal.di.Heuristic
import javax.inject.Inject

enum class AutofillSaveDraftRejection {
    PACKAGE_MISSING,
    PASSWORD_MISSING,
    PASSWORD_AMBIGUOUS,
}

sealed interface AutofillSaveDraftExtraction {
    data class Ready(val draft: AutofillSaveDraft) : AutofillSaveDraftExtraction
    data class Rejected(val reason: AutofillSaveDraftRejection) : AutofillSaveDraftExtraction
}

class AutofillSaveDraftExtractor @Inject constructor(
    @param:Heuristic private val fieldMatchStrategy: FieldMatchStrategy,
) {
    operator fun invoke(request: AutofillRequest): AutofillSaveDraftExtraction {
        val packageName = request.packageName.trim()
        if (packageName.isEmpty()) {
            return AutofillSaveDraftExtraction.Rejected(
                AutofillSaveDraftRejection.PACKAGE_MISSING,
            )
        }

        val roles = fieldMatchStrategy.match(request).roleMap
        val password = when (val selection = selectValue(request, roles, FieldRole.PASSWORD)) {
            is ValueSelection.Selected -> selection.value
            ValueSelection.Missing -> return AutofillSaveDraftExtraction.Rejected(
                AutofillSaveDraftRejection.PASSWORD_MISSING,
            )
            ValueSelection.Ambiguous -> return AutofillSaveDraftExtraction.Rejected(
                AutofillSaveDraftRejection.PASSWORD_AMBIGUOUS,
            )
        }
        val username = when (val selection = selectValue(request, roles, FieldRole.USERNAME)) {
            is ValueSelection.Selected -> selection.value
            ValueSelection.Missing,
            ValueSelection.Ambiguous -> ""
        }

        return AutofillSaveDraftExtraction.Ready(
            AutofillSaveDraft(
                packageName = packageName,
                webDomain = request.domain,
                pageTitle = null,
                username = username,
                password = password,
            ),
        )
    }

    private fun selectValue(
        request: AutofillRequest,
        roles: Map<String, FieldRole>,
        role: FieldRole,
    ): ValueSelection {
        val candidates = request.fields.filter { field ->
            roles[field.id] == role && !field.text.isNullOrBlank()
        }
        val focusedCandidates = candidates.filter { it.isFocused }
        val selected = when {
            focusedCandidates.size == 1 -> focusedCandidates.single()
            focusedCandidates.size > 1 -> return ValueSelection.Ambiguous
            candidates.size == 1 -> candidates.single()
            candidates.isEmpty() -> return ValueSelection.Missing
            else -> return ValueSelection.Ambiguous
        }
        return ValueSelection.Selected(selected.text.orEmpty())
    }

    private sealed interface ValueSelection {
        data class Selected(val value: String) : ValueSelection
        data object Missing : ValueSelection
        data object Ambiguous : ValueSelection
    }
}

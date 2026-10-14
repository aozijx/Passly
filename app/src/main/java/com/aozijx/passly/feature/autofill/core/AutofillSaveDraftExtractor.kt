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
        val values = request.fields.associate { it.id to it.text.orEmpty() }
        val password = roles.entries.firstNotNullOfOrNull { (id, role) ->
            values[id]?.takeIf(String::isNotBlank)?.takeIf { role == FieldRole.PASSWORD }
        } ?: return AutofillSaveDraftExtraction.Rejected(
            AutofillSaveDraftRejection.PASSWORD_MISSING,
        )
        val username = roles.entries.firstNotNullOfOrNull { (id, role) ->
            values[id]?.takeIf(String::isNotBlank)?.takeIf { role == FieldRole.USERNAME }
        }.orEmpty()

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
}

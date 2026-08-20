package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.model.AutofillConstants
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.feature.autofill.internal.matcher.rule.FieldRoleClassifier
import com.aozijx.passly.feature.autofill.internal.matcher.rule.IdKeywordRule
import com.aozijx.passly.feature.autofill.internal.matcher.rule.InputTypeRule
import com.aozijx.passly.feature.autofill.internal.matcher.rule.StandardHintRule
import com.aozijx.passly.feature.autofill.internal.matcher.rule.TextContentRule
import javax.inject.Inject

/**
 * Heuristic match strategy for AutofillService.
 */
class HeuristicMatchStrategy @Inject constructor(
    private val hintProvider: AutofillHintProvider
) : FieldMatchStrategy {

    override fun match(request: AutofillRequest): MatchResult {
        val roleMap = mutableMapOf<String, FieldRole>()
        var hasPasswordTermField = false
        var hasLoginTermField = false
        var focusedEditable: AutofillField? = null

        val usernamePattern = hintProvider.getUsernamePattern()
        val passwordPattern = hintProvider.getPasswordPattern()
        val otpPattern = hintProvider.getOtpPattern()
        val searchPattern = hintProvider.getSearchPattern()
        val confirmationPattern = hintProvider.getConfirmationPattern()
        val submitPattern = hintProvider.getSubmitPattern()

        val rules = listOf(
            StandardHintRule(hintProvider),
            InputTypeRule(confirmationPattern),
            IdKeywordRule(usernamePattern, passwordPattern, otpPattern, confirmationPattern),
            TextContentRule(usernamePattern, passwordPattern, otpPattern, confirmationPattern, submitPattern)
        )

        for (field in request.fields) {
            // 1. Skip search fields early
            if (isSearchField(field, searchPattern)) continue

            // 2. Determine field role using rule chain
            val role = classifyField(field, rules)
            if (role != FieldRole.UNKNOWN) {
                roleMap[field.id] = role
            }

            // 3. Track state for weak-target synthesis
            val isEditable = isEditableTextField(field)
            if (field.isFocused && isEditable) {
                focusedEditable = field
            }

            // 4. Scan for overall page context signals
            if (!hasPasswordTermField || !hasLoginTermField) {
                if (
                    !hasPatternInField(field, confirmationPattern) &&
                    hasPatternInField(field, passwordPattern)
                ) {
                    hasPasswordTermField = true
                }
                if (hasPatternInField(field, usernamePattern)) hasLoginTermField = true
            }
        }

        // Trigger decision
        val identifiedAnyRole = roleMap.values.any {
            it == FieldRole.USERNAME || it == FieldRole.PASSWORD || it == FieldRole.OTP
        }

        val hasCredentials = identifiedAnyRole || hasPasswordTermField || (hasLoginTermField && focusedEditable != null)

        // Login context synthesis
        if (hasCredentials && identifiedAnyRole && focusedEditable != null && focusedEditable.id !in roleMap) {
            roleMap[focusedEditable.id] = when {
                focusedEditable.inputType?.let { raw ->
                    AutofillConstants.PASSWORD_INPUT_TYPES.any { it in raw }
                } == true -> FieldRole.PASSWORD
                else -> FieldRole.USERNAME
            }
        }

        return MatchResult(
            roleMap = roleMap,
            hasCredentials = hasCredentials,
        )
    }

    private fun classifyField(field: AutofillField, rules: List<FieldRoleClassifier>): FieldRole {
        for (rule in rules) {
            val role = rule.classify(field)
            if (role != FieldRole.UNKNOWN) return role
        }
        return FieldRole.UNKNOWN
    }

    private fun isSearchField(field: AutofillField, searchPattern: Regex): Boolean {
        val resourceId = field.resourceId.orEmpty()
        val rawId = resourceId.substringAfterLast("/", resourceId).lowercase()
        val hint = field.hints.joinToString(" ")
        val contentDesc = field.contentDescription.orEmpty()
        return searchPattern.containsMatchIn(rawId) ||
               searchPattern.containsMatchIn(hint) ||
               searchPattern.containsMatchIn(contentDesc)
    }

    private fun hasPatternInField(field: AutofillField, pattern: Regex): Boolean {
        return field.resourceId?.let { pattern.containsMatchIn(it) } == true ||
               field.hints.any { pattern.containsMatchIn(it) } ||
               field.contentDescription?.let { pattern.containsMatchIn(it) } == true
    }

    private fun isEditableTextField(field: AutofillField): Boolean {
        if (field.hints.isNotEmpty()) return true
        val className = field.className.orEmpty()
        if (className.contains("edittext", ignoreCase = true) || className.contains("textinput", ignoreCase = true)) return true
        val raw = field.inputType ?: return false
        return raw.contains("TYPE_CLASS_TEXT") || raw.contains("TYPE_CLASS_NUMBER")
    }
}

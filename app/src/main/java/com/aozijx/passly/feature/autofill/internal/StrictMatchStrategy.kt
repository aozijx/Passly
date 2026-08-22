package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.model.AutofillConstants
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import javax.inject.Inject

/**
 * Strict match strategy for CredentialProviderService.
 */
class StrictMatchStrategy @Inject constructor(
    private val hintProvider: AutofillHintProvider
) : FieldMatchStrategy {

    override fun match(request: AutofillRequest): MatchResult {
        val roleMap = mutableMapOf<String, FieldRole>()
        val hintRoleMap = hintProvider.getHintRoleMap()
        val usernamePattern = hintProvider.getUsernamePattern()
        val passwordPattern = hintProvider.getPasswordPattern()
        val otpPattern = hintProvider.getOtpPattern()
        val submitPattern = hintProvider.getSubmitPattern()

        for (field in request.fields) {
            val role = matchField(field, hintRoleMap, usernamePattern, passwordPattern, otpPattern, submitPattern)
            if (role != FieldRole.UNKNOWN) {
                roleMap[field.id] = role
            }
        }

        val hasCredentials = roleMap.values.any {
            it == FieldRole.USERNAME || it == FieldRole.PASSWORD || it == FieldRole.OTP
        }

        return MatchResult(
            roleMap = roleMap,
            hasCredentials = hasCredentials,
        )
    }

    private fun matchField(
        field: AutofillField,
        hintRoleMap: Map<String, FieldRole>,
        usernamePattern: Regex,
        passwordPattern: Regex,
        otpPattern: Regex,
        submitPattern: Regex
    ): FieldRole {
        // 1. Standard Autofill Hints
        for (hint in field.hints) {
            hintRoleMap[AutofillConstants.normalizeHint(hint)]?.let { return it }
        }

        // 2. Resource ID Matching
        val rawId = field.resourceId.orEmpty()
        val shortId = rawId.substringAfterLast("/", rawId)

        if (passwordPattern.containsMatchIn(shortId)) return FieldRole.PASSWORD
        if (otpPattern.containsMatchIn(shortId)) return FieldRole.OTP
        if (usernamePattern.containsMatchIn(shortId)) return FieldRole.USERNAME

        // 3. Submit Button Identification
        val cls = field.className.orEmpty().lowercase()
        val isButtonLike = cls.contains("button") || cls.contains("imageview") || cls.contains("textview")
        if (isButtonLike) {
            if (submitPattern.containsMatchIn(shortId)) return FieldRole.SUBMIT

            val combinedText = "${field.text.orEmpty()} ${field.contentDescription.orEmpty()}"
            if (combinedText.isNotBlank() && submitPattern.containsMatchIn(combinedText)) {
                return FieldRole.SUBMIT
            }
        }

        return FieldRole.UNKNOWN
    }
}

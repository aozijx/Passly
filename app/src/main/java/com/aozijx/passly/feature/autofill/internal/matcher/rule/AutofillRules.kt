package com.aozijx.passly.feature.autofill.internal.matcher.rule

import com.aozijx.passly.domain.autofill.model.AutofillConstants
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider

/**
 * Priority 1: Standard Autofill Hints (Highest confidence)
 */
class StandardHintRule(private val hintProvider: AutofillHintProvider) : FieldRoleClassifier {
    override fun classify(field: AutofillField): FieldRole {
        val hintRoleMap = hintProvider.getHintRoleMap()
        for (hint in field.hints) {
            hintRoleMap[AutofillConstants.normalizeHint(hint)]?.let { return it }
        }
        return FieldRole.UNKNOWN
    }
}

/**
 * Priority 2: Input Type (High confidence for passwords)
 */
class InputTypeRule(private val confirmationPattern: Regex) : FieldRoleClassifier {
    override fun classify(field: AutofillField): FieldRole {
        val inputType = field.inputType.orEmpty()
        val isPwdType = AutofillConstants.PASSWORD_INPUT_TYPES.any { it in inputType }
        val isUserType = !isPwdType && AutofillConstants.USERNAME_INPUT_TYPES.any { it in inputType }

        if (isPwdType) {
            val resourceId = field.resourceId.orEmpty()
            val rawId = resourceId.substringAfterLast("/", resourceId)
            val combinedText = "${field.hint.orEmpty()} ${field.contentDescription.orEmpty()}".lowercase()
            val isConfirmation = confirmationPattern.containsMatchIn(rawId) ||
                                confirmationPattern.containsMatchIn(combinedText)
            return if (isConfirmation) FieldRole.UNKNOWN else FieldRole.PASSWORD
        }
        if (isUserType) return FieldRole.USERNAME

        return FieldRole.UNKNOWN
    }
}

/**
 * Priority 3: Resource ID / View Name
 */
class IdKeywordRule(
    private val usernamePattern: Regex,
    private val passwordPattern: Regex,
    private val otpPattern: Regex,
    private val confirmationPattern: Regex
) : FieldRoleClassifier {
    override fun classify(field: AutofillField): FieldRole {
        val rawId = field.resourceId.orEmpty()
        val shortId = rawId.substringAfterLast("/", rawId)

        if (passwordPattern.containsMatchIn(shortId)) {
            val combinedText = "${field.hint.orEmpty()} ${field.contentDescription.orEmpty()}".lowercase()
            val isConfirmation = confirmationPattern.containsMatchIn(shortId) ||
                                confirmationPattern.containsMatchIn(combinedText)
            return if (isConfirmation) FieldRole.UNKNOWN else FieldRole.PASSWORD
        }
        if (otpPattern.containsMatchIn(shortId)) return FieldRole.OTP
        if (usernamePattern.containsMatchIn(shortId)) return FieldRole.USERNAME

        return FieldRole.UNKNOWN
    }
}

/**
 * Priority 4: Text Matching (Hint/ContentDesc)
 */
class TextContentRule(
    private val usernamePattern: Regex,
    private val passwordPattern: Regex,
    private val otpPattern: Regex,
    private val confirmationPattern: Regex,
    private val submitPattern: Regex
) : FieldRoleClassifier {
    override fun classify(field: AutofillField): FieldRole {
        val className = field.className.orEmpty().lowercase()
        val hintText = field.hint.orEmpty()
        val contentDesc = field.contentDescription.orEmpty()
        val combinedText = "$hintText $contentDesc".lowercase()
        val resourceId = field.resourceId.orEmpty()
        val rawId = resourceId.substringAfterLast("/", resourceId)

        val isButtonLike = className.contains("button") || className.contains("imageview")
        if (!isButtonLike) {
            if (passwordPattern.containsMatchIn(combinedText)) {
                val isConfirmation = confirmationPattern.containsMatchIn(rawId) ||
                                    confirmationPattern.containsMatchIn(combinedText)
                return if (isConfirmation) FieldRole.UNKNOWN else FieldRole.PASSWORD
            }
            if (usernamePattern.containsMatchIn(combinedText)) return FieldRole.USERNAME
            if (otpPattern.containsMatchIn(combinedText)) return FieldRole.OTP
        } else {
            if (submitPattern.containsMatchIn(combinedText) || submitPattern.containsMatchIn(rawId)) {
                return FieldRole.SUBMIT
            }
        }
        return FieldRole.UNKNOWN
    }
}

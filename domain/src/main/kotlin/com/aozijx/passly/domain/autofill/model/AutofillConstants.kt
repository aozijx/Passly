package com.aozijx.passly.domain.autofill.model

/**
 * Shared constants for autofill field identification and processing.
 */
object AutofillConstants {

    val PASSWORD_INPUT_TYPES = setOf(
        "TEXT_VARIATION_PASSWORD",
        "TEXT_VARIATION_VISIBLE_PASSWORD",
        "TEXT_VARIATION_WEB_PASSWORD",
        "NUMBER_VARIATION_PASSWORD",
    )

    val USERNAME_INPUT_TYPES = setOf(
        "TEXT_VARIATION_EMAIL_ADDRESS",
        "TEXT_VARIATION_PERSON_NAME",
        "TEXT_VARIATION_WEB_EMAIL_ADDRESS",
        "TEXT_VARIATION_WEB_EDIT_TEXT",
    )

    /**
     * Normalizes an autofill hint string by removing non-alphanumeric characters and converting to lowercase.
     */
    fun normalizeHint(hint: String): String =
        hint.lowercase().filter(Char::isLetterOrDigit)
}

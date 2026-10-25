package com.aozijx.passly.feature.autofill.core

import android.text.InputType

/** Android's text and number variations share bits, so the class must be checked first. */
internal object AutofillInputTypeClassifier {
    fun describe(inputType: Int): String {
        if (inputType == InputType.TYPE_NULL) return "TYPE_NULL"
        val typeClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val names = mutableListOf<String>()
        when (typeClass) {
            InputType.TYPE_CLASS_TEXT -> names += "TYPE_CLASS_TEXT"
            InputType.TYPE_CLASS_NUMBER -> names += "TYPE_CLASS_NUMBER"
            InputType.TYPE_CLASS_PHONE -> names += "TYPE_CLASS_PHONE"
            InputType.TYPE_CLASS_DATETIME -> names += "TYPE_CLASS_DATETIME"
        }
        when (typeClass) {
            InputType.TYPE_CLASS_TEXT -> when (variation) {
                InputType.TYPE_TEXT_VARIATION_NORMAL -> names += "TEXT_VARIATION_NORMAL"
                InputType.TYPE_TEXT_VARIATION_PASSWORD -> names += "TEXT_VARIATION_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> names += "TEXT_VARIATION_VISIBLE_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> names += "TEXT_VARIATION_WEB_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> names += "TEXT_VARIATION_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> names += "TEXT_VARIATION_PERSON_NAME"
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> names += "TEXT_VARIATION_WEB_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> names += "TEXT_VARIATION_WEB_EDIT_TEXT"
            }
            InputType.TYPE_CLASS_NUMBER -> if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                names += "NUMBER_VARIATION_PASSWORD"
            }
        }
        return names.joinToString(" ")
    }

    fun isPassword(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}

package com.aozijx.passly.feature.autofill.core

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInputTypeClassifierTest {
    @Test
    fun numberPasswordKeepsNumberVariation() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        assertTrue(AutofillInputTypeClassifier.describe(inputType).contains("NUMBER_VARIATION_PASSWORD"))
        assertFalse(AutofillInputTypeClassifier.describe(inputType).contains("TEXT_VARIATION_PASSWORD"))
        assertTrue(AutofillInputTypeClassifier.isPassword(inputType))
    }

    @Test
    fun textPasswordVariationsKeepTheirNamesAndArePasswords() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD to "TEXT_VARIATION_PASSWORD",
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD to "TEXT_VARIATION_VISIBLE_PASSWORD",
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD to "TEXT_VARIATION_WEB_PASSWORD",
        ).forEach { (variation, expectedName) ->
            val inputType = InputType.TYPE_CLASS_TEXT or variation

            assertTrue(AutofillInputTypeClassifier.describe(inputType).contains(expectedName))
            assertTrue(AutofillInputTypeClassifier.isPassword(inputType))
        }
    }

    @Test
    fun textUriIsNotMistakenForNumberPassword() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        assertFalse(AutofillInputTypeClassifier.describe(inputType).contains("NUMBER_VARIATION_PASSWORD"))
        assertFalse(AutofillInputTypeClassifier.isPassword(inputType))
    }
}

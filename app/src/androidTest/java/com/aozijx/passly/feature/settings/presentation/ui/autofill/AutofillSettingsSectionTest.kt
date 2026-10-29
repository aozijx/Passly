package com.aozijx.passly.feature.settings.presentation.ui.autofill

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel
import org.junit.Rule
import org.junit.Test

class AutofillSettingsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savePromptAndPresentationOptionsRemainAvailableWithoutCredentialManagerSupport() {
        val settings = AutofillSettingsUiModel(
            enabled = true,
            presentation = AutofillPresentation.SYSTEM_INLINE,
            credentialManagerEnabled = false,
            supportsCredentialManager = false,
            requireAuthentication = true,
            includeOtp = true,
            savePromptsEnabled = true,
            allowUnmatchedSuggestions = false,
            maxSuggestions = 5,
            minSuggestions = 1,
            maxSuggestionsLimit = 10,
            isSystemServiceEnabled = true,
        )
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = settings,
                    onAction = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithText(
            context.getString(R.string.settings_autofill_save_prompts),
        ).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.settings_autofill_presentation))
            .performClick()
        listOf(
            R.string.settings_autofill_mode_floating,
            R.string.settings_autofill_mode_inline,
            R.string.settings_autofill_mode_bottom_sheet,
        ).forEach { label ->
            composeRule.onNode(hasText(context.getString(label)) and hasAnyAncestor(isDialog()))
                .assertIsDisplayed()
        }
    }
}

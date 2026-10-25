package com.aozijx.passly.presentation.feature.settings.ui.autofill

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsUiModel
import org.junit.Rule
import org.junit.Test

class AutofillSettingsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun legacySaveSwitchRemainsVisibleWithoutCredentialManagerSupport() {
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = AutofillSettingsUiModel(
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
                    ),
                    onAction = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithText(
            context.getString(R.string.settings_autofill_save_prompts),
        ).assertIsDisplayed()
    }

    @Test
    fun presentationPickerOffersFloatingInlineAndBottomSheet() {
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = AutofillSettingsUiModel(
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
                    ),
                    onAction = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithText(context.getString(R.string.settings_autofill_presentation))
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_autofill_mode_floating))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_autofill_mode_inline))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_autofill_mode_bottom_sheet))
            .assertIsDisplayed()
    }
}

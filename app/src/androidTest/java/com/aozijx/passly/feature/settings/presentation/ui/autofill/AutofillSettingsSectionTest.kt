package com.aozijx.passly.feature.settings.presentation.ui.autofill

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsAction
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSourceState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutofillSettingsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun presentationOptionsRemainAvailableAndEmitSelectedValueWithoutCredentialManagerSupport() {
        val actions = mutableListOf<AutofillSettingsAction>()
        val settings =
            AutofillSettings(
                enabled = true,
                presentation = AutofillPresentation.FLOATING,
                credentialManagerEnabled = false,
                requireAuthentication = true,
                strictDomainMatching = true,
            )
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = settings,
                    supportsCredentialManager = false,
                    systemService = AutofillSourceState.Ready(true),
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_presentation))
            .performClick()
        listOf(
            R.string.settings_autofill_mode_floating,
            R.string.settings_autofill_mode_inline,
            R.string.settings_autofill_mode_bottom_sheet,
        ).forEach { label ->
            composeRule
                .onNode(hasText(context.getString(label)) and hasAnyAncestor(isDialog()))
                .assertIsDisplayed()
        }

        composeRule
            .onNode(hasText(context.getString(R.string.settings_autofill_mode_bottom_sheet)) and hasAnyAncestor(isDialog()))
            .performClick()

        assertEquals(
            listOf(AutofillSettingsAction.SetPresentation(AutofillPresentation.BOTTOM_SHEET)),
            actions,
        )
    }

    @Test
    fun systemStatusFailureIsUnknownAndCanBeRetried() {
        val actions = mutableListOf<AutofillSettingsAction>()
        val systemService = AutofillSourceState.Failed(false)
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = settings(),
                    supportsCredentialManager = false,
                    systemService = systemService,
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_system_status_unavailable))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_system_disabled))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()

        assertEquals(
            listOf(AutofillSettingsAction.RetrySystemService),
            actions,
        )
    }

    @Test
    fun strictDomainMatchingToggleEmitsItsDedicatedAction() {
        val actions = mutableListOf<AutofillSettingsAction>()
        composeRule.setContent {
            MaterialTheme {
                AutofillSettingsSection(
                    settings = settings(),
                    supportsCredentialManager = false,
                    systemService = AutofillSourceState.Ready(true),
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_strict_domain_matching))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_strict_domain_matching_summary))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_autofill_strict_domain_matching))
            .performClick()

        assertEquals(listOf(AutofillSettingsAction.SetStrictDomainMatching(false)), actions)
    }

    private fun settings() =
        AutofillSettings(
            enabled = true,
            presentation = AutofillPresentation.FLOATING,
            credentialManagerEnabled = false,
            requireAuthentication = true,
            strictDomainMatching = true,
        )
}

package com.aozijx.passly.feature.settings.presentation.ui.appearance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsAction
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsUiMapper
import com.aozijx.passly.presentation.shared.entry.labelRes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CardStyleSettingsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardStyleSectionStartsCollapsedAndUsesEqualPreviewFrames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loginDefault = "card-style-preview-login-default"
        val loginPassword = "card-style-preview-login-password"
        val otpDefault = "card-style-preview-otp-default"
        val otpTotp = "card-style-preview-otp-totp"
        val loginPasswordCard = "card-style-option-login-password"
        val actions = mutableListOf<InterfaceSettingsAction>()

        composeRule.setContent {
            MaterialTheme {
                InterfaceDetail(
                    state =
                        InterfaceSettingsUiMapper.map(
                            settings =
                                InterfaceSettings(
                                    hideSystemBars = false,
                                    collapseTopBarOnScroll = false,
                                    collapseQuickFilterBarOnScroll = false,
                                    appCornerRadiusDp = 16f,
                                ),
                            librarySettings =
                                LibraryViewSettings(
                                    sort = EntrySort.presets().first(),
                                    entryCardPresentations = emptyList(),
                                    entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                                ),
                            otpPolicy = OtpRuntimePolicy(),
                        ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onAllNodesWithTag(loginDefault).assertCountEquals(0)
        composeRule
            .onNodeWithText(context.getString(R.string.settings_interface_card_style_section))
            .performClick()

        composeRule.onAllNodesWithTag(loginDefault).assertCountEquals(0)
        composeRule.onAllNodesWithTag(loginPassword).assertCountEquals(0)
        composeRule.onAllNodesWithTag(otpDefault).assertCountEquals(0)
        composeRule.onAllNodesWithTag(otpTotp).assertCountEquals(0)

        composeRule.onNodeWithText(context.getString(EntryType.LOGIN.labelRes)).performClick()
        composeRule.onNodeWithTag(loginDefault).assertIsDisplayed()
        composeRule.onNodeWithTag(loginPassword).assertIsDisplayed()
        composeRule.onAllNodesWithTag(otpDefault).assertCountEquals(0)
        composeRule.onAllNodesWithTag(otpTotp).assertCountEquals(0)

        composeRule.onNodeWithText(context.getString(EntryType.OTP.labelRes)).performClick()
        composeRule.onNodeWithTag(loginDefault).assertIsDisplayed()
        composeRule.onNodeWithTag(loginPassword).assertIsDisplayed()
        composeRule.onNodeWithTag(otpDefault).assertIsDisplayed()
        composeRule.onNodeWithTag(otpTotp).assertIsDisplayed()
        composeRule.onAllNodesWithText("PASSWORD").assertCountEquals(0)

        val defaultSize = composeRule.onNodeWithTag(loginDefault).fetchSemanticsNode().size
        listOf(loginPassword, otpDefault, otpTotp).forEach { tag ->
            val size = composeRule.onNodeWithTag(tag).fetchSemanticsNode().size
            assertEquals(defaultSize.width, size.width)
            assertEquals(defaultSize.height, size.height)
        }

        val passwordCardSize = composeRule.onNodeWithTag(loginPasswordCard).fetchSemanticsNode().size
        composeRule
            .onNodeWithTag(loginPasswordCard)
            .performTouchInput {
                click(Offset(passwordCardSize.width.toFloat() - 2f, passwordCardSize.height / 2f))
            }
        assertEquals(
            listOf(
                InterfaceSettingsAction.SetEntryCardPresentation(
                    EntryCardPresentation(
                        entryTypeKey = "login",
                        variantKey = "password",
                        density = CardDensity.STANDARD,
                        showIcon = true,
                        showFavorite = true,
                        showSecondaryText = true,
                        showQuickAction = true,
                    ),
                ),
            ),
            actions,
        )
    }
}

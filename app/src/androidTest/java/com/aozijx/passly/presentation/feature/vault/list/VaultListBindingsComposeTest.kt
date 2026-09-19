package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultListBindingsComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun callbackRecompositionKeepsItemHandlerWhileUsingLatestCallback() {
        val callbackVersion = mutableIntStateOf(0)
        val events = mutableListOf<String>()
        lateinit var currentHandler: VaultListItemEventHandler

        composeRule.setContent {
            val version = callbackVersion.intValue
            currentHandler = rememberVaultListItemEventHandler(
                onItemClick = { events += "$version:${it.id}" },
                onItemSwipe = { _, _ -> },
            )
        }

        lateinit var initialHandler: VaultListItemEventHandler
        composeRule.runOnIdle { initialHandler = currentHandler }
        callbackVersion.intValue = 1
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertSame(initialHandler, currentHandler)
            currentHandler.onClick(vaultListItem("entry-1"))
            assertEquals(listOf("1:entry-1"), events)
        }
    }

    @Test
    fun callbackRecompositionKeepsEventHandlerWhileUsingLatestCallback() {
        val callbackVersion = mutableIntStateOf(0)
        val events = mutableListOf<String>()
        lateinit var currentHandler: VaultListEventHandler

        composeRule.setContent {
            val version = callbackVersion.intValue
            currentHandler = rememberVaultListEventHandler(
                onEvent = { events += "$version:$it" },
            )
        }

        lateinit var initialHandler: VaultListEventHandler
        composeRule.runOnIdle { initialHandler = currentHandler }
        callbackVersion.intValue = 1
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertSame(initialHandler, currentHandler)
            currentHandler.onEvent(VaultListEvent.SettingsClicked)
            assertEquals(listOf("1:SettingsClicked"), events)
        }
    }

    private fun vaultListItem(id: String) = VaultListItemUiModel(
        id = id,
        entryType = EntryTypeUiModel.LOGIN,
        title = "Mail",
        username = "user@example.com",
        category = null,
        favorite = false,
        associatedDomain = null,
        associatedAppPackage = null,
        iconName = null,
        iconCustomPath = null,
        hasPassword = true,
        hasOtp = false,
        otpKind = null,
        otpPreview = null,
    )
}

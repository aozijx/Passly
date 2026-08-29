package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.mutableIntStateOf
import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import kotlinx.coroutines.flow.flowOf
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
    fun callbackRecompositionKeepsBindingsAndPagingFlowWhileUsingLatestCallback() {
        val callbackVersion = mutableIntStateOf(0)
        val passwordPages = flowOf(PagingData.empty<VaultListItemUiModel>())
        val entryPages = mapOf(VaultQuickFilterUiModel.PASSWORDS to passwordPages)
        val events = mutableListOf<String>()
        lateinit var currentBindings: VaultListBindings

        composeRule.setContent {
            val version = callbackVersion.intValue
            currentBindings = rememberVaultListBindings(
                entryPages = entryPages,
                onItemClick = { events += "$version:${it.id}" },
                onItemSwipe = { _, _ -> },
            )
        }

        lateinit var initialBindings: VaultListBindings
        composeRule.runOnIdle { initialBindings = currentBindings }
        callbackVersion.intValue = 1
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertSame(initialBindings, currentBindings)
            assertSame(
                passwordPages,
                currentBindings.entryPages.getValue(VaultQuickFilterUiModel.PASSWORDS),
            )
            currentBindings.eventHandler.onClick(vaultListItem("entry-1"))
            assertEquals(listOf("1:entry-1"), events)
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

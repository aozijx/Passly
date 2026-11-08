package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedEntryUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordUiModel
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.R
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class RelatedEntriesSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun swipingDirectPasswordRequestsConfirmation() {
        val actions = mutableListOf<DetailUiAction>()
        showSection(actions = actions)

        compose.onNodeWithTag("related-password-row-target").performTouchInput { swipeLeft() }

        compose.runOnIdle {
            assertEquals(
                listOf(DetailUiAction.RequestPasswordUnlink("target", "link")),
                actions,
            )
        }
    }

    @Test
    fun dismissingConfirmationLeavesRelationAndRow() {
        val actions = mutableListOf<DetailUiAction>()
        showSection(
            actions = actions,
            unlinkState = confirming(),
        )

        compose.onNodeWithText(text(R.string.cancel)).performClick()

        compose.runOnIdle { assertEquals(listOf(DetailUiAction.DismissPasswordUnlink), actions) }
        compose.onNodeWithText("Portal").assertIsDisplayed()
    }

    @Test
    fun confirmingAndRetryingUnlinkDispatchesActions() {
        val actions = mutableListOf<DetailUiAction>()
        showSection(
            actions = actions,
            unlinkState = confirming(RelatedPasswordUnlinkUiState.Phase.Failed),
        )

        compose.onNodeWithText(text(R.string.vault_detail_unlink_failed)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.vault_detail_unlink_confirm)).performClick()

        compose.runOnIdle { assertEquals(listOf(DetailUiAction.ConfirmPasswordUnlink), actions) }
    }

    @Test
    fun accountGroupAndDirectRelationRenderSeparately() {
        showSection(
            accounts = listOf(RelatedEntryUiModel("account", "Shared item", EntryType.LOGIN)),
            passwords = listOf(RelatedPasswordUiModel("link", "target", "Shared item", "person")),
        )

        compose.onAllNodesWithText("Shared item").assertCountEquals(2)
        compose.onNodeWithText("person").assertIsDisplayed()
    }

    @Test
    fun accountGroupRowHasNoUnlinkAccessibilityAction() {
        showSection(accounts = listOf(RelatedEntryUiModel("account", "Account row", EntryType.LOGIN)))

        compose.onNodeWithTag("related-entry-row-account").assertHasClickAction()
        val config = compose.onNodeWithTag("related-entry-row-account").fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsActions.CustomActions))
    }

    @Test
    fun accessibilityCustomActionRequestsConfirmation() {
        val actions = mutableListOf<DetailUiAction>()
        showSection(actions = actions)

        val customAction = compose.onNodeWithTag("related-password-row-target")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        customAction.action()

        compose.runOnIdle {
            assertEquals(
                listOf(DetailUiAction.RequestPasswordUnlink("target", "link")),
                actions,
            )
        }
    }

    private fun showSection(
        actions: MutableList<DetailUiAction> = mutableListOf(),
        accounts: List<RelatedEntryUiModel> = emptyList(),
        other: List<RelatedEntryUiModel> = emptyList(),
        passwords: List<RelatedPasswordUiModel> = listOf(RelatedPasswordUiModel("link", "target", "Portal", "person")),
        unlinkState: RelatedPasswordUnlinkUiState = RelatedPasswordUnlinkUiState.Closed,
    ) {
        compose.setContent {
            MaterialTheme {
                RelatedEntriesSection(
                    accountGroupEntries = accounts,
                    otherRelatedEntries = other,
                    relatedPasswords = passwords,
                    canBindPasswords = true,
                    unlinkState = unlinkState,
                    onAction = actions::add,
                    onOpenEntry = {},
                )
            }
        }
    }

    private fun confirming(phase: RelatedPasswordUnlinkUiState.Phase = RelatedPasswordUnlinkUiState.Phase.Ready) =
        RelatedPasswordUnlinkUiState.Confirming("target", "link", "Portal", phase)

    private fun text(resourceId: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}

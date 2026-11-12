package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingOperationState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordCandidateUiModel
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordCandidateUsernameUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RelatedPasswordPickerSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyCandidatesAndNoSearchMatchesHaveDifferentMessages() {
        val state =
            mutableStateOf<RelatedPasswordBindingUiState>(
                RelatedPasswordBindingUiState.Ready(emptyList()),
            )
        compose.setContent {
            MaterialTheme {
                RelatedPasswordPickerSheet(state = state.value, onAction = {})
            }
        }
        compose.onNodeWithText(text(R.string.vault_detail_bind_password_empty)).assertIsDisplayed()

        compose.runOnIdle {
            state.value =
                RelatedPasswordBindingUiState.Ready(
                    candidates = listOf(RelatedPasswordCandidateUiModel("id", "Portal", RelatedPasswordCandidateUsernameUiState.Hidden)),
                    query = "missing",
                )
        }
        compose.onNodeWithText(text(R.string.vault_detail_bind_password_no_matches)).assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        val actions = mutableListOf<DetailUiAction>()
        show(RelatedPasswordBindingUiState.LoadFailed, actions)

        compose.onNodeWithText(text(R.string.retry)).performClick()

        compose.runOnIdle { assertEquals(listOf(DetailUiAction.RetryPasswordBinding), actions) }
    }

    @Test
    fun activeBindShowsProgressOnItsRow() {
        show(
            RelatedPasswordBindingUiState.Ready(
                candidates =
                    listOf(
                        RelatedPasswordCandidateUiModel(
                            "id",
                            "Portal",
                            RelatedPasswordCandidateUsernameUiState.Visible("person"),
                        ),
                    ),
                operation = RelatedPasswordBindingOperationState.Saving("id"),
            ),
        )

        compose.onNodeWithText("Portal").assertIsDisplayed()
        compose.onNodeWithTag("related-password-saving").assertIsDisplayed()
    }

    private fun show(
        state: RelatedPasswordBindingUiState,
        actions: MutableList<DetailUiAction> = mutableListOf(),
    ) {
        compose.setContent {
            MaterialTheme { RelatedPasswordPickerSheet(state = state, onAction = actions::add) }
        }
    }

    private fun text(resourceId: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}

package com.aozijx.passly.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryAffectedFieldsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryLoadState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryRevisionReference
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionFieldLabelUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionRevealActionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailHistorySheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision.RevisionDifferenceRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailHistorySheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyHistoryAndComparisonWrapTheirContentAndKeepRestoreAvailable() {
        val model =
            mutableStateOf<DetailHistorySheetUiModel>(
                DetailHistorySheetUiModel.ListPage(historyList()),
            )
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                DetailHistorySheet(
                    model.value,
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.revision_history_empty)).assertIsDisplayed()
        assertSheetWrapsContent("Empty history")

        model.value =
            DetailHistorySheetUiModel.ComparisonPage(
                "revision-1",
                DetailRevisionComparisonUiState.Ready(DetailRevisionComparisonContentUiState.NoChanges, DetailRevisionRestorePhase.Idle),
            )
        compose.waitForIdle()
        compose
            .onNodeWithText(context.getString(R.string.revision_restore_action))
            .assertIsDisplayed()
            .performClick()
        assertEquals(DetailUiAction.RequestRevisionRestore, actions.last())
        assertSheetWrapsContent("Empty comparison")
    }

    @Test
    fun longHistorySelectionOpensScrollableComparisonWithRestoreAvailable() {
        val revisions =
            (1..50).map {
                DetailHistoryItemUiModel(
                    id = "revision:revision-$it",
                    description =
                        DetailHistoryDescriptionUiState.Snapshot(
                            version = it,
                            changeLabelRes = R.string.revision_change_changed,
                        ),
                    icon = DetailHistoryIcon.SNAPSHOT,
                    filter = DetailHistoryFilter.CONTENT_CHANGES,
                    affectedFields = DetailHistoryAffectedFieldsUiState.None,
                    createdAtMs = it.toLong(),
                    revision = DetailHistoryRevisionReference.Linked(it, "revision-$it"),
                )
            }
        val differences =
            (1..30).map {
                DetailRevisionDifferenceUiModel(
                    field = RevisionFieldId.CustomField(it, "Field $it"),
                    label = DetailRevisionFieldLabelUiState.Named(R.string.revision_custom_field_label, "Field $it"),
                    changeLabelRes = R.string.revision_change_changed,
                    content =
                        DetailRevisionDifferenceContentUiState.Values(
                            before = DetailRevisionValueUiModel.Text("Before $it"),
                            after = DetailRevisionValueUiModel.Text("After $it"),
                        ),
                    revealAction = DetailRevisionRevealActionUiState.Unavailable,
                )
            }
        val model =
            mutableStateOf<DetailHistorySheetUiModel>(
                DetailHistorySheetUiModel.ListPage(historyList(revisions)),
            )
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                DetailHistorySheet(
                    model.value,
                    onAction = { action ->
                        actions += action
                        if (action is DetailUiAction.SelectRevision) {
                            model.value =
                                DetailHistorySheetUiModel.ComparisonPage(
                                    action.revisionId,
                                    DetailRevisionComparisonUiState.Ready(
                                        DetailRevisionComparisonContentUiState.Changes(differences),
                                        DetailRevisionRestorePhase.Idle,
                                    ),
                                )
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("detail-history-list").performScrollToIndex(51)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose
            .onNodeWithText(context.getString(R.string.revision_version_format, 50))
            .assertIsDisplayed()
            .performClick()
        assertEquals(DetailUiAction.SelectRevision("revision-50"), actions.last())
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.revision_restore_action)).assertIsDisplayed()
        compose.onNodeWithTag("revision-comparison-list").performScrollToIndex(29)
        compose.onNodeWithText(context.getString(R.string.revision_custom_field_label, "Field 30")).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.revision_restore_action)).assertIsDisplayed()
    }

    @Test
    fun revealIsOfferedOnlyForAFieldPresentInTheHistoricalHighSensitivityRows() {
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                Column {
                    RevisionDifferenceRow(
                        DetailRevisionDifferenceUiModel(
                            field = RevisionFieldId.ProtectedField(FieldKey.PASSWORD),
                            label = DetailRevisionFieldLabelUiState.Resource(R.string.password_label),
                            changeLabelRes = R.string.revision_change_changed,
                            content = DetailRevisionDifferenceContentUiState.ProtectedHidden,
                            revealAction = DetailRevisionRevealActionUiState.Unavailable,
                        ),
                        actions::add,
                    )
                    RevisionDifferenceRow(
                        DetailRevisionDifferenceUiModel(
                            field = RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD),
                            label = DetailRevisionFieldLabelUiState.Resource(R.string.password_label),
                            changeLabelRes = R.string.revision_change_added,
                            content = DetailRevisionDifferenceContentUiState.ProtectedHidden,
                            revealAction = DetailRevisionRevealActionUiState.Unavailable,
                        ),
                        actions::add,
                    )
                    RevisionDifferenceRow(
                        DetailRevisionDifferenceUiModel(
                            field = RevisionFieldId.Sensitive(SensitiveFieldKey.CARD_CVV),
                            label = DetailRevisionFieldLabelUiState.Resource(R.string.card_cvv),
                            changeLabelRes = R.string.revision_change_removed,
                            content = DetailRevisionDifferenceContentUiState.ProtectedHidden,
                            revealAction = DetailRevisionRevealActionUiState.Available(SensitiveFieldKey.CARD_CVV),
                        ),
                        actions::add,
                    )
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose
            .onAllNodesWithText(context.getString(R.string.revision_reveal_action))
            .assertCountEquals(1)
            .onFirst()
            .performClick()
        assertEquals(DetailUiAction.RevealRevisionField(SensitiveFieldKey.CARD_CVV), actions.single())
    }

    @Test
    fun restoreInProgressCannotNavigateBackOrDismissTheSheet() {
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                DetailHistorySheet(
                    DetailHistorySheetUiModel.ComparisonPage(
                        "revision-1",
                        DetailRevisionComparisonUiState.Ready(
                            DetailRevisionComparisonContentUiState.NoChanges,
                            DetailRevisionRestorePhase.Restoring,
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        compose.onNodeWithContentDescription(context.getString(R.string.back)).assertIsNotEnabled()
        compose.onNodeWithTag("detail-history-sheet").performTouchInput { swipeDown() }

        assertTrue(actions.isEmpty())
    }

    private fun assertSheetWrapsContent(description: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sheet = compose.onNodeWithTag("detail-history-sheet").getUnclippedBoundsInRoot()
        assertTrue(
            "$description should wrap its content: $sheet",
            (sheet.bottom.value - sheet.top.value) < context.resources.configuration.screenHeightDp * 0.6f,
        )
    }

    private fun historyList(items: List<DetailHistoryItemUiModel> = emptyList()) =
        DetailHistoryListUiState(
            filter = DetailHistoryFilter.ALL,
            items = items,
            activitySource = DetailHistoryLoadState.READY,
            revisionSource = DetailHistoryLoadState.READY,
        )
}

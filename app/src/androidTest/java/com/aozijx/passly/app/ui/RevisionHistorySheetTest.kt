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
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RevisionHistorySheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision.RevisionDifferenceRow
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionValueUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionSheetUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RevisionHistorySheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyHistoryAndComparisonWrapTheirContentAndKeepRestoreAvailable() {
        val model =
            mutableStateOf<DetailRevisionSheetUiModel>(
                DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.ReadyEmpty),
            )
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    model.value,
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.revision_history_empty)).assertIsDisplayed()
        assertSheetWrapsContent("Empty history")

        model.value =
            DetailRevisionSheetUiModel.ComparisonPage(
                "revision-1",
                DetailRevisionComparisonUiState.Ready(emptyList(), DetailRevisionRestorePhase.Idle),
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
                DetailRevisionItemUiModel("revision-$it", it, 1L, RevisionChange.VALUE_CHANGED)
            }
        val differences =
            (1..30).map {
                DetailRevisionDifferenceUiModel(
                    RevisionFieldId.CustomField(it, "Field $it"),
                    RevisionDifferenceKind.CHANGED,
                    DetailRevisionValueUiModel.Text("Before $it"),
                    DetailRevisionValueUiModel.Text("After $it"),
                )
            }
        val model =
            mutableStateOf<DetailRevisionSheetUiModel>(
                DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.ReadyItems(revisions)),
            )
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    model.value,
                    onAction = { action ->
                        actions += action
                        if (action is DetailUiAction.SelectRevision) {
                            model.value =
                                DetailRevisionSheetUiModel.ComparisonPage(
                                    action.revisionId,
                                    DetailRevisionComparisonUiState.Ready(
                                        differences,
                                        DetailRevisionRestorePhase.Idle,
                                    ),
                                )
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("revision-history-list").performScrollToIndex(49)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onAllNodesWithText(context.getString(R.string.revision_compare_action)).onLast().performClick()
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
                            RevisionFieldId.ProtectedField(FieldKey.PASSWORD),
                            RevisionDifferenceKind.CHANGED,
                            DetailRevisionValueUiModel.Absent,
                            DetailRevisionValueUiModel.Absent,
                        ),
                        actions::add,
                    )
                    RevisionDifferenceRow(
                        DetailRevisionDifferenceUiModel(
                            RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD),
                            RevisionDifferenceKind.ADDED,
                            DetailRevisionValueUiModel.Absent,
                            DetailRevisionValueUiModel.Absent,
                        ),
                        actions::add,
                    )
                    RevisionDifferenceRow(
                        DetailRevisionDifferenceUiModel(
                            RevisionFieldId.Sensitive(SensitiveFieldKey.CARD_CVV),
                            RevisionDifferenceKind.REMOVED,
                            DetailRevisionValueUiModel.Absent,
                            DetailRevisionValueUiModel.Absent,
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
                RevisionHistorySheet(
                    DetailRevisionSheetUiModel.ComparisonPage(
                        "revision-1",
                        DetailRevisionComparisonUiState.Ready(
                            emptyList(),
                            DetailRevisionRestorePhase.Restoring,
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        compose.onNodeWithContentDescription(context.getString(R.string.back)).assertIsNotEnabled()
        compose.onNodeWithTag("revision-history-sheet").performTouchInput { swipeDown() }

        assertTrue(actions.isEmpty())
    }

    private fun assertSheetWrapsContent(description: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sheet = compose.onNodeWithTag("revision-history-sheet").getUnclippedBoundsInRoot()
        assertTrue(
            "$description should wrap its content: $sheet",
            (sheet.bottom.value - sheet.top.value) < context.resources.configuration.screenHeightDp * 0.6f,
        )
    }
}

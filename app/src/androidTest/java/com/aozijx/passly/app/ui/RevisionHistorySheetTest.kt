package com.aozijx.passly.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionItemUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionRestoreUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision.RevisionDifferenceRow
import java.io.File
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RevisionHistorySheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionSheetUiModel
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RevisionHistorySheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyHistoryDoesNotReserveAnAlmostFullScreenBlankArea() {
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.Ready(emptyList())),
                    onAction = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.revision_history_empty)).assertIsDisplayed()
        captureSheet("history-empty.png")
        val sheet = compose.onNodeWithTag("revision-history-sheet").getUnclippedBoundsInRoot()
        assertTrue("Empty history should wrap its content: $sheet",
            (sheet.bottom.value - sheet.top.value) < context.resources.configuration.screenHeightDp * 0.6f)
    }

    @Test
    fun longHistoryCanReachAndSelectItsLastRevision() {
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.Ready(
                        (1..50).map { DetailRevisionItemUiModel("revision-$it", it, 1L, RevisionChange.VALUE_CHANGED) },
                    )),
                    onAction = actions::add,
                )
            }
        }
        compose.onNodeWithTag("revision-history-list").performScrollToIndex(49)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onAllNodesWithText(context.getString(R.string.revision_compare_action)).onLast().performClick()
        assertEquals(DetailUiAction.SelectRevision("revision-50"), actions.last())
    }

    @Test
    fun comparisonWithoutChangesKeepsRestoreVisibleAndWrapsContent() {
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    DetailRevisionSheetUiModel.ComparisonPage("revision-1",
                        DetailRevisionComparisonUiState.Ready(emptyList(), DetailRevisionRestoreUiState.Idle)),
                    onAction = actions::add,
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.revision_restore_action)).assertIsDisplayed().performClick()
        assertEquals(DetailUiAction.RequestRevisionRestore, actions.last())
        val sheet = compose.onNodeWithTag("revision-history-sheet").getUnclippedBoundsInRoot()
        assertTrue("Empty comparison should wrap its content: $sheet",
            (sheet.bottom.value - sheet.top.value) < context.resources.configuration.screenHeightDp * 0.6f)
    }

    @Test
    fun longComparisonKeepsRestoreVisibleWhileAllDifferencesCanBeReached() {
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    DetailRevisionSheetUiModel.ComparisonPage("revision-1", DetailRevisionComparisonUiState.Ready(
                        (1..30).map { DetailRevisionDifferenceUiModel(
                            RevisionFieldId.CustomField(it, "Field $it"), RevisionDifferenceKind.CHANGED,
                            RevisionFieldValue.Text("Before $it"), RevisionFieldValue.Text("After $it"),
                        ) }, DetailRevisionRestoreUiState.Idle,
                    )), onAction = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.revision_restore_action)).assertIsDisplayed()
        compose.onNodeWithTag("revision-comparison-list").performScrollToIndex(29)
        compose.onNodeWithText(context.getString(R.string.revision_custom_field_label, "Field 30")).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.revision_restore_action)).assertIsDisplayed()
        captureSheet("history-long-comparison.png")
    }

    @Test
    fun revealIsOfferedOnlyForAFieldPresentInTheHistoricalHighSensitivityRows() {
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                Column {
                    RevisionDifferenceRow(DetailRevisionDifferenceUiModel(
                        RevisionFieldId.ProtectedField(FieldKey.PASSWORD), RevisionDifferenceKind.CHANGED, null, null), actions::add)
                    RevisionDifferenceRow(DetailRevisionDifferenceUiModel(
                        RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD), RevisionDifferenceKind.ADDED, null, null), actions::add)
                    RevisionDifferenceRow(DetailRevisionDifferenceUiModel(
                        RevisionFieldId.Sensitive(SensitiveFieldKey.CARD_CVV), RevisionDifferenceKind.REMOVED, null, null), actions::add)
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onAllNodesWithText(context.getString(R.string.revision_reveal_action)).assertCountEquals(1).onFirst().performClick()
        assertEquals(DetailUiAction.RevealRevisionField(SensitiveFieldKey.CARD_CVV), actions.single())
    }

    private fun captureSheet(name: String) {
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = arguments.getString("additionalTestOutputDir")?.let(::File)
            ?: checkNotNull(context.getExternalFilesDir(null))
        directory.mkdirs()
        File(directory, name).outputStream().use {
            compose.onNodeWithTag("revision-history-sheet").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

package com.aozijx.passly.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import android.graphics.Bitmap
import android.graphics.Color
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
    fun emptyHistoryAndComparisonWrapTheirContentAndKeepRestoreAvailable() {
        val model = mutableStateOf<DetailRevisionSheetUiModel>(
            DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.Ready(emptyList())),
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
        captureSheet("history-empty.png")
        assertSheetWrapsContent("Empty history")

        model.value = DetailRevisionSheetUiModel.ComparisonPage(
            "revision-1",
            DetailRevisionComparisonUiState.Ready(emptyList(), DetailRevisionRestoreUiState.Idle),
        )
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.revision_restore_action))
            .assertIsDisplayed()
            .performClick()
        assertEquals(DetailUiAction.RequestRevisionRestore, actions.last())
        assertSheetWrapsContent("Empty comparison")
    }

    @Test
    fun longHistorySelectionOpensScrollableComparisonWithRestoreAvailable() {
        val revisions = (1..50).map {
            DetailRevisionItemUiModel("revision-$it", it, 1L, RevisionChange.VALUE_CHANGED)
        }
        val differences = (1..30).map {
            DetailRevisionDifferenceUiModel(
                RevisionFieldId.CustomField(it, "Field $it"),
                RevisionDifferenceKind.CHANGED,
                RevisionFieldValue.Text("Before $it"),
                RevisionFieldValue.Text("After $it"),
            )
        }
        val model = mutableStateOf<DetailRevisionSheetUiModel>(
            DetailRevisionSheetUiModel.ListPage(DetailRevisionListUiState.Ready(revisions)),
        )
        val actions = mutableListOf<DetailUiAction>()
        compose.setContent {
            MaterialTheme {
                RevisionHistorySheet(
                    model.value,
                    onAction = { action ->
                        actions += action
                        if (action is DetailUiAction.SelectRevision) {
                            model.value = DetailRevisionSheetUiModel.ComparisonPage(
                                action.revisionId,
                                DetailRevisionComparisonUiState.Ready(
                                    differences,
                                    DetailRevisionRestoreUiState.Idle,
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
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = arguments.getString("additionalTestOutputDir")?.let(::File)
            ?: checkNotNull(context.getExternalFilesDir(null))
        directory.mkdirs()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var captured: Bitmap? = null
        repeat(20) { attempt ->
            if (captured != null) return@repeat
            val candidate = automation.takeScreenshot()
            val readable = candidate.copy(Bitmap.Config.ARGB_8888, false)
            val top = readable.getPixel(readable.width / 2, readable.height / 2)
            val bottom = readable.getPixel(readable.width / 2, readable.height * 85 / 100)
            val showsSheet = name != "history-empty.png" ||
                (Color.red(bottom) - Color.red(top) > 30 && Color.green(bottom) - Color.green(top) > 30)
            readable.recycle()
            if (showsSheet) {
                captured = candidate
            } else {
                candidate.recycle()
                if (attempt < 19) Thread.sleep(100)
            }
        }
        val screenshot = checkNotNull(captured) { "Captured revision sheet is blank: $name" }
        File(directory, name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
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

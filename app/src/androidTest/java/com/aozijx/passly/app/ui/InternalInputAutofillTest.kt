package com.aozijx.passly.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.feature.backup.presentation.ui.BackupImportOptionsContent
import com.aozijx.passly.feature.backup.presentation.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordUiState
import com.aozijx.passly.feature.vault.presentation.editor.ui.password.AddPasswordEditorScreen
import com.aozijx.passly.presentation.shared.components.apppassword.PasswordFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InternalInputAutofillTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun applicationPasswordInputsAreExplicitlyNotAutofillable() {
        compose.setContent {
            MaterialTheme {
                Column { PasswordFields("", "", {}, {}, {}) }
            }
        }
        assertEquals(2, assertInputsExcluded())
        val showPassword = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.show_password)
        compose.onAllNodesWithContentDescription(showPassword).onFirst().performClick()
        assertEquals(2, assertInputsExcluded())
    }

    @Test
    fun credentialEditorInputsAreExplicitlyNotAutofillable() {
        compose.setContent {
            MaterialTheme {
                AddPasswordEditorScreen(
                    state = AddPasswordUiState(form = AddPasswordFormState()),
                    onAction = {}, onBack = {},
                    snackbarHostState = remember { SnackbarHostState() },
                )
            }
        }
        assertInputsExcluded()
    }

    @Test
    fun backupPasswordIsExplicitlyNotAutofillable() {
        compose.setContent {
            MaterialTheme {
                BackupImportOptionsContent(
                    BackupRestoreSheetUiState.ImportOptions("", ImportMode.APPEND, BackupImportStrategy.COMPATIBLE),
                    onAction = {},
                )
            }
        }
        assertEquals(1, assertInputsExcluded())
    }

    @Test
    fun nativeHostExcludesItsWholeViewSubtree() {
        val roots = mutableListOf<View>()
        compose.setContent {
            val view = LocalView.current
            SideEffect {
                view.context.activity().excludeFromAutofill()
                roots += view.rootView
            }
        }
        compose.runOnIdle {
            assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS, roots.last().importantForAutofill)
        }
    }

    private fun assertInputsExcluded(): Int {
        val inputs = compose.onAllNodes(hasSetTextAction(), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("No actual editable nodes were inspected", inputs.isNotEmpty())
        inputs.forEach { input ->
            assertEquals(ContentDataType.None, input.config.getOrNull(SemanticsProperties.ContentDataType))
        }
        return inputs.size
    }

    private tailrec fun Context.activity(): Activity = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> error("Test content has no Activity")
    }
}

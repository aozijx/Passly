package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIconCardUiModel
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategory
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconGlyphUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconUiModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailIconCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clickingCardContentOpensIconEditorWithoutSeparateEditButton() {
        var editorOpened = false
        val model =
            DetailIconCardUiModel(
                icon =
                    VaultItemIconUiModel(
                        image = VaultItemIconImageUiState.Default,
                        glyph = VaultItemIconGlyphUiState.CategoryFallback,
                        associatedApp = VaultItemAssociatedAppUiState.None,
                        visualCategory = EntryVisualCategory.PERSONAL,
                    ),
                iconDescriptionRes = R.string.vault_detail_favicon_default,
            )
        val title =
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.vault_detail_favicon_title)
        compose.setContent {
            MaterialTheme {
                DetailIconCard(model = model, onEdit = { editorOpened = true })
            }
        }

        compose.onNodeWithText(title).performClick()

        assertTrue(editorOpened)
    }
}
